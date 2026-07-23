package ru.badmintonlab.worker.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON-ответ AJAX-пагинации справочника игроков ({@code POST /?ajax}, тело {@code players={token}&limit=N}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayersDirectoryAjaxResponse(
        String err,
        String html,
        @JsonProperty("showAll") int showAll
) {
    public boolean isComplete() {
        return showAll != 0;
    }

    public boolean isOk() {
        return err == null || err.isEmpty();
    }
}
