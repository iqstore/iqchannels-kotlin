package ru.iqchannels.sdk.schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class RelationMap {
    public final Map<Long, Client> Clients;
    public final Map<Long, ChatMessage> ChatMessages;
    public final Map<String, UploadedFile> Files;
    public final Map<Long, Rating> Ratings;
    public final Map<Long, User> Users;

    public RelationMap() {
        Clients = new HashMap<>();
        ChatMessages = new HashMap<>();
        Files = new HashMap<>();
        Ratings = new HashMap<>();
        Users = new HashMap<>();
    }
}
