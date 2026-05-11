package com.streamrec.dto;

import java.util.List;

public record StandardApiResponse<T>(
        boolean success,
        T data,
        List<String> errors
) {

    public static <T> StandardApiResponse<T> success(T data) {
        return new StandardApiResponse<>(true, data, List.of());
    }
}
