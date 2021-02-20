package ru.iqchannels.sdk.schema;


import androidx.annotation.Nullable;

import java.util.List;

/**
 * Copyright iqstore.ru. All Rights Reserved.
 */
public class Relations {
    @Nullable public List<ChatMessage> ChatMessages;
    @Nullable public List<Client> Clients;
    @Nullable public List<UploadedFile> Files;
    @Nullable public List<Rating> Ratings;
    @Nullable public List<User> Users;
}
