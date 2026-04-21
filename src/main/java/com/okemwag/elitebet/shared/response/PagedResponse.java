package com.okemwag.elitebet.shared.response;

import java.util.List;

public record PagedResponse<T>(List<T> data, long totalElements, int page, int size) {
}
