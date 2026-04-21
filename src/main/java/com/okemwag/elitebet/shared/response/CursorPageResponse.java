package com.okemwag.elitebet.shared.response;

import java.util.List;

public record CursorPageResponse<T>(List<T> data, String nextCursor) {
}
