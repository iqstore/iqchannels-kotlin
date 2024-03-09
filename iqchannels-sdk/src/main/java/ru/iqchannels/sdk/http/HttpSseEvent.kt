package ru.iqchannels.sdk.http;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class HttpSseEvent {
    @NonNull public final String id;
    @NonNull public final String name;
    @NonNull public final String data;

    HttpSseEvent(@Nullable String id, @Nullable String name, @Nullable String data) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.data = data != null ? data : "";
    }
}
