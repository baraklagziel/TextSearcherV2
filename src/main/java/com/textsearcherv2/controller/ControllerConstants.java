package com.textsearcherv2.controller;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class ControllerConstants {
    public static final int CHUNK_SIZE_LIMIT = 1000; // Specify Chunk Size

    public static final String V1 = "/v1";
    public static final String PROCESS_URL = "/process-url";
    public static final int CORES = Runtime.getRuntime().availableProcessors();
    public static final String FILE_ID_PATH_VAR = "/{fileId}";
    public static final String ERROR = "/errors";

}
