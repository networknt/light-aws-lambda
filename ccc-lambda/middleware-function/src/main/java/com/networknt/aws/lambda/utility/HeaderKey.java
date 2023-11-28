package com.networknt.aws.lambda.utility;

import java.util.Collections;
import java.util.List;

public class HeaderKey {

    /* unique header keys used by light-4j */
    public static final String TRACEABILITY = "x-traceability-id";
    public static final String CORRELATION = "x-correlation-id";
    public static final String AUTHORIZATION = "Authorization";

    public static final String SCOPE_TOKEN = "X-Scope-Token";
    /* common header keys */
    public static final String CONTENT_TYPE = "Content-Type";

    /* Amazon header keys */
    public static final String PARAMETER_SECRET_TOKEN = "X-Aws-Parameters-Secrets-Token";
    public static final String AMZ_TARGET = "X-Amz-Target";


}
