package com.networknt.aws.lambda;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

final class LambdaRuntimeApiEndpoint {
    private static final String RUNTIME_API_SCHEME = "http";
    private static final String INVOCATION_BASE_PATH = "/2018-06-01/runtime/invocation";
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Pattern IPV4_PATTERN = Pattern.compile("\\d{1,3}(?:\\.\\d{1,3}){3}");

    private LambdaRuntimeApiEndpoint() {
    }

    static URI nextInvocation(String endpoint) {
        return runtimeApiUri(endpoint, INVOCATION_BASE_PATH + "/next");
    }

    static URI invocationResponse(String endpoint, String requestId) {
        return runtimeApiUri(endpoint, INVOCATION_BASE_PATH + "/" + validateRequestId(requestId) + "/response");
    }

    static URI invocationError(String endpoint, String requestId) {
        return runtimeApiUri(endpoint, INVOCATION_BASE_PATH + "/" + validateRequestId(requestId) + "/error");
    }

    static String validateRequestId(String requestId) {
        if (requestId == null || !REQUEST_ID_PATTERN.matcher(requestId).matches()) {
            throw new IllegalArgumentException("Invalid Lambda runtime request id.");
        }
        return requestId;
    }

    private static URI runtimeApiUri(String endpoint, String path) {
        URI baseUri = parseEndpoint(endpoint);
        try {
            return new URI(RUNTIME_API_SCHEME, null, baseUri.getHost(), baseUri.getPort(), path, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Lambda runtime API endpoint.", e);
        }
    }

    private static URI parseEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("AWS_LAMBDA_RUNTIME_API is required.");
        }

        URI uri;
        try {
            uri = new URI(RUNTIME_API_SCHEME + "://" + endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Lambda runtime API endpoint.", e);
        }

        if (uri.getUserInfo() != null || uri.getHost() == null || uri.getPort() < 1 || uri.getPort() > 65535
                || hasText(uri.getRawPath()) || hasText(uri.getRawQuery()) || hasText(uri.getRawFragment())) {
            throw new IllegalArgumentException("AWS_LAMBDA_RUNTIME_API must be a host:port value.");
        }

        if (!isAllowedRuntimeApiHost(uri.getHost())) {
            throw new IllegalArgumentException("AWS_LAMBDA_RUNTIME_API host is not allowed.");
        }

        return uri;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isAllowedRuntimeApiHost(String host) {
        String normalizedHost = normalizeHost(host);

        if ("localhost".equalsIgnoreCase(normalizedHost)) {
            return true;
        }

        if (IPV4_PATTERN.matcher(normalizedHost).matches()) {
            return isAllowedIpv4Host(normalizedHost);
        }

        String lowerCaseHost = normalizedHost.toLowerCase();
        return "::1".equals(lowerCaseHost)
                || "0:0:0:0:0:0:0:1".equals(lowerCaseHost)
                || lowerCaseHost.startsWith("fe80:");
    }

    private static String normalizeHost(String host) {
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static boolean isAllowedIpv4Host(String host) {
        String[] segments = host.split("\\.");
        int[] octets = new int[segments.length];
        for (int i = 0; i < segments.length; i++) {
            octets[i] = parseOctet(segments[i]);
            if (octets[i] < 0) {
                return false;
            }
        }

        return octets[0] == 127 || (octets[0] == 169 && octets[1] == 254);
    }

    private static int parseOctet(String segment) {
        if (segment.length() > 1 && segment.startsWith("0")) {
            return -1;
        }

        int value;
        try {
            value = Integer.parseInt(segment);
        } catch (NumberFormatException e) {
            return -1;
        }
        return value >= 0 && value <= 255 ? value : -1;
    }
}
