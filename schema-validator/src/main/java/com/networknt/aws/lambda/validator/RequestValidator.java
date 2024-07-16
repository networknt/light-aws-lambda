package com.networknt.aws.lambda.validator;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.config.Config;
import com.networknt.jsonoverlay.Overlay;
import com.networknt.oas.model.Parameter;
import com.networknt.oas.model.RequestBody;
import com.networknt.oas.model.impl.RequestBodyImpl;
import com.networknt.oas.model.impl.SchemaImpl;
import com.networknt.openapi.NormalisedPath;
import com.networknt.openapi.OpenApiOperation;
import com.networknt.openapi.ValidatorConfig;
import com.networknt.schema.PathType;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.status.Status;
import com.networknt.utility.MapUtil;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.*;

import static com.networknt.aws.lambda.utility.HeaderKey.CONTENT_TYPE;
import static java.util.Objects.requireNonNull;

public class RequestValidator {
    static final Logger LOG = LoggerFactory.getLogger(RequestValidator.class);
    static final String VALIDATOR_REQUEST_BODY_UNEXPECTED = "ERR11013";
    static final String VALIDATOR_REQUEST_BODY_MISSING = "ERR11014";
    static final String VALIDATOR_REQUEST_PARAMETER_HEADER_MISSING = "ERR11017";
    static final String VALIDATOR_REQUEST_PARAMETER_QUERY_MISSING = "ERR11000";
    static final String CONTENT_TYPE_MISMATCH = "ERR10015";

    private final SchemaValidator schemaValidator;
    private final ValidatorConfig validatorConfig;

    /**
     * Construct a new request validator with the given Open API validator.
     *
     * @param schemaValidator The schema validator to use when validating request
     */
    public RequestValidator(final SchemaValidator schemaValidator, final ValidatorConfig validatorConfig) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
        this.validatorConfig = requireNonNull(validatorConfig, "A validator config is required");
    }

    /**
     * Validate the request against the given API operation
     * @param requestPath normalised path
     * @param requestEvent The APIGatewayProxyRequestEvent to validate
     * @param openApiOperation OpenAPI operation
     * @return A validation report containing validation errors
     */
    public Status validateRequest(final NormalisedPath requestPath, APIGatewayProxyRequestEvent requestEvent, OpenApiOperation openApiOperation) {
        requireNonNull(requestPath, "A request path is required");
        requireNonNull(requestEvent, "A request event is required");
        requireNonNull(openApiOperation, "An OpenAPI operation is required");

        Status status = validateRequestParameters(requestEvent, requestPath, openApiOperation);
        if(status != null) return status;
        String contentType = requestEvent.getHeaders() == null ? null : requestEvent.getHeaders().get(CONTENT_TYPE);
        if (contentType==null || contentType.startsWith("application/json")) {
            String body = requestEvent.getBody();
            // skip the body validation if body parser is not in the request chain.
            if(body == null || validatorConfig.isSkipBodyValidation()) return null;
            status = validateRequestBody(body, openApiOperation);
        }
        return status;
    }

    private Status validateRequestBody(String requestBody, final OpenApiOperation openApiOperation) {
        final RequestBody specBody = openApiOperation.getOperation().getRequestBody();

        if (requestBody != null && specBody == null) {
            return new Status(VALIDATOR_REQUEST_BODY_UNEXPECTED, openApiOperation.getMethod(), openApiOperation.getPathString().original());
        }

        if (specBody == null || !Overlay.isPresent((RequestBodyImpl)specBody)) {
            return null;
        }

        if (requestBody == null) {
            if (specBody.getRequired() != null && specBody.getRequired()) {
                return new Status(VALIDATOR_REQUEST_BODY_MISSING, openApiOperation.getMethod(), openApiOperation.getPathString().original());
            }
            return null;
        }
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder()
                .typeLoose(false)
                .pathType(PathType.JSON_POINTER)
                .nullableKeywordEnabled(validatorConfig.isHandleNullableField())
                .build();
        // the body can be converted to JsonNode here. If not, an error is returned.
        JsonNode requestNode = null;
        requestBody = requestBody.trim();
        if(requestBody.startsWith("{") || requestBody.startsWith("[")) {
            try {
                requestNode = Config.getInstance().getMapper().readTree(requestBody);
            } catch (Exception e) {
                return new Status(CONTENT_TYPE_MISMATCH, "application/json");
            }
        } else {
            return new Status(CONTENT_TYPE_MISMATCH, "application/json");
        }
        return schemaValidator.validate(requestNode, Overlay.toJson((SchemaImpl)specBody.getContentMediaType("application/json").getSchema()), config);
    }

    private Status validateRequestParameters(final APIGatewayProxyRequestEvent requestEvent, final NormalisedPath requestPath, final OpenApiOperation openApiOperation) {
        Status status = validatePathParameters(requestEvent, requestPath, openApiOperation);
        if(status != null) return status;

        status = validateQueryParameters(requestEvent, openApiOperation);
        if(status != null) return status;

        status = validateHeaderParameters(requestEvent, openApiOperation);
        if(status != null) return status;

        return null;
    }

    private Status validatePathParameters(final APIGatewayProxyRequestEvent requestEvent, final NormalisedPath requestPath, final OpenApiOperation openApiOperation) {
        ValidationResult result = validateDeserializedValues(requestEvent, openApiOperation.getOperation().getParameters(), ParameterType.PATH);

        if (null!=result.getStatus() || result.getSkippedParameters().isEmpty()) {
            return result.getStatus();
        }

        // validate values that cannot be deserialized or do not need to be deserialized
        Status status = null;
        for (int i = 0; i < openApiOperation.getPathString().parts().size(); i++) {
            if (!openApiOperation.getPathString().isParam(i)) {
                continue;
            }

            final String paramName = openApiOperation.getPathString().paramName(i);
            final Optional<Parameter> parameter = result.getSkippedParameters()
                    .stream()
                    .filter(p -> p.getName().equalsIgnoreCase(paramName))
                    .findFirst();

            if (parameter.isPresent()) {
                String paramValue = requestPath.part(i); // If it can't be UTF-8 decoded, use directly.
                try {
                    paramValue = URLDecoder.decode(requestPath.part(i), "UTF-8");
                } catch (Exception e) {
                    LOG.info("Path parameter cannot be decoded, it will be used directly");
                }

                return schemaValidator.validate(new TextNode(paramValue), Overlay.toJson((SchemaImpl)(parameter.get().getSchema())), paramName);
            }
        }
        return status;
    }

    private Status validateQueryParameters(final APIGatewayProxyRequestEvent requestEvent,
                                           final OpenApiOperation openApiOperation) {
        ValidationResult result = validateDeserializedValues(requestEvent, openApiOperation.getOperation().getParameters(), ParameterType.QUERY);

        if (null!=result.getStatus() || result.getSkippedParameters().isEmpty()) {
            return result.getStatus();
        }

        // validate values that cannot be deserialized or do not need to be deserialized
        Optional<Status> optional = result.getSkippedParameters()
                .stream()
                .map(p -> validateQueryParameter(requestEvent, openApiOperation, p))
                .filter(s -> s != null)
                .findFirst();

        return optional.orElse(null);
    }


    private Status validateQueryParameter(final APIGatewayProxyRequestEvent requestEvent,
                                          final OpenApiOperation openApiOperation,
                                          final Parameter queryParameter) {

        String queryParameterValue = requestEvent.getQueryStringParameters() != null ? requestEvent.getQueryStringParameters().get(queryParameter.getName()) : null;
        if ((queryParameterValue == null || queryParameterValue.isEmpty())) {
            if(queryParameter.getRequired() != null && queryParameter.getRequired()) {
                return new Status(VALIDATOR_REQUEST_PARAMETER_QUERY_MISSING, queryParameter.getName(), openApiOperation.getPathString().original());
            }
        } else {
            return schemaValidator.validate(new TextNode(queryParameterValue), Overlay.toJson((SchemaImpl)queryParameter.getSchema()), queryParameter.getName());
        }
        return null;
    }

    private Status validateHeaderParameters(final APIGatewayProxyRequestEvent requestEvent, final OpenApiOperation openApiOperation) {
        // validate path level parameters for headers first.
        Optional<Status> optional = validatePathLevelHeaders(requestEvent, openApiOperation);
        if(optional.isPresent()) {
            return optional.get();
        } else {
            // validate operation level parameter for headers second.
            optional = validateOperationLevelHeaders(requestEvent, openApiOperation);
            return optional.orElse(null);
        }
    }

    private Optional<Status> validatePathLevelHeaders(final APIGatewayProxyRequestEvent requestEvent, final OpenApiOperation openApiOperation) {
        ValidationResult result = validateDeserializedValues(requestEvent, openApiOperation.getPathObject().getParameters(), ParameterType.HEADER);

        if (null!=result.getStatus() || result.getSkippedParameters().isEmpty()) {
            return Optional.ofNullable(result.getStatus());
        }

        return result.getSkippedParameters().stream()
                .map(p -> validateHeader(requestEvent, openApiOperation, p))
                .filter(s -> s != null)
                .findFirst();
    }



    private Optional<Status> validateOperationLevelHeaders(final APIGatewayProxyRequestEvent requestEvent, final OpenApiOperation openApiOperation) {
        ValidationResult result = validateDeserializedValues(requestEvent, openApiOperation.getOperation().getParameters(), ParameterType.HEADER);

        if (null!=result.getStatus() || result.getSkippedParameters().isEmpty()) {
            return Optional.ofNullable(result.getStatus());
        }

        return result.getSkippedParameters().stream()
                .map(p -> validateHeader(requestEvent, openApiOperation, p))
                .filter(s -> s != null)
                .findFirst();
    }

    private Status validateHeader(final APIGatewayProxyRequestEvent requestEvent,
                                  final OpenApiOperation openApiOperation,
                                  final Parameter headerParameter) {
        final Optional<String> headerValue = MapUtil.getValueIgnoreCase(requestEvent.getHeaders(), headerParameter.getName());
        if (headerValue.isEmpty()) {
            if(headerParameter.getRequired()) {
                return new Status(VALIDATOR_REQUEST_PARAMETER_HEADER_MISSING, headerParameter.getName(), openApiOperation.getPathString().original());
            }
        } else {
            return headerParameter.getSchema() != null ? schemaValidator.validate(new TextNode(headerValue.get()), Overlay.toJson((SchemaImpl)headerParameter.getSchema()), headerParameter.getName()) : null;
        }
        return null;
    }


    private ValidationResult validateDeserializedValues(final APIGatewayProxyRequestEvent requestEvent, final Collection<Parameter> parameters, final ParameterType type) {
        ValidationResult validationResult = new ValidationResult();

        parameters.stream()
                .filter(p -> ParameterType.is(p.getIn(), type))
                .forEach(p->{
                    String deserializedValue = getDeserializedValue(requestEvent, p.getName(), type);
                    if (null==deserializedValue) {
                        validationResult.addSkipped(p);
                    }else {
                        Status s = schemaValidator.validate(new TextNode(deserializedValue), Overlay.toJson((SchemaImpl)(p.getSchema())), p.getName());
                        validationResult.addStatus(s);
                    }
                });

        return validationResult;
    }

    private String getDeserializedValue(final APIGatewayProxyRequestEvent requestEvent, final String name, final ParameterType type) {
        if (null != type && StringUtils.isNotBlank(name)) {
            switch(type){
                case QUERY:
                    return requestEvent.getQueryStringParameters() != null ? requestEvent.getQueryStringParameters().get(name) : null;
                case PATH:
                    return requestEvent.getPathParameters() != null ? requestEvent.getPathParameters().get(name) : null;
                case HEADER:
                    return requestEvent.getHeaders() != null ? requestEvent.getHeaders().get(name) : null;
            }
        }
        return null;
    }


    static class ValidationResult {
        private final Set<Parameter> skippedParameters = new HashSet<>();;
        private final List<Status> statuses = new ArrayList<>();

        public void addSkipped(Parameter p) {
            skippedParameters.add(p);
        }

        public void addStatus(Status s) {
            if (null!=s) {
                statuses.add(s);
            }
        }

        public Set<Parameter> getSkippedParameters(){
            return Collections.unmodifiableSet(skippedParameters);
        }

        public Status getStatus() {
            return statuses.isEmpty()?null:statuses.get(0);
        }

        public List<Status> getAllStatuses(){
            return Collections.unmodifiableList(statuses);
        }
    }
}
