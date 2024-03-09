/*
 * Copyright (c) 2017 iqstore.ru.
 * All rights reserved.
 */

package ru.iqchannels.sdk.rels;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import ru.iqchannels.sdk.schema.ChatEvent;
import ru.iqchannels.sdk.schema.ChatMessage;
import ru.iqchannels.sdk.schema.Client;
import ru.iqchannels.sdk.schema.ClientAuth;
import ru.iqchannels.sdk.schema.FileImageSize;
import ru.iqchannels.sdk.schema.FileType;
import ru.iqchannels.sdk.schema.Rating;
import ru.iqchannels.sdk.schema.Relations;
import ru.iqchannels.sdk.schema.UploadedFile;
import ru.iqchannels.sdk.schema.RelationMap;
import ru.iqchannels.sdk.schema.User;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 26/01/2017.
 */

public class Rels {
    private final String address;

    public Rels(String address) {
        if (address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }

        this.address = address;
    }

    // RelationMap

    public RelationMap map(Relations rels) {
        RelationMap map = new RelationMap();
        if (rels.ChatMessages != null) {
            for (ChatMessage message : rels.ChatMessages) {
                map.ChatMessages.put(message.Id, message);
            }
        }
        if (rels.Clients != null) {
            for (Client client : rels.Clients) {
                map.Clients.put(client.Id, client);
            }
        }
        if (rels.Files != null) {
            for (UploadedFile file : rels.Files) {
                map.Files.put(file.Id, file);
            }
        }
        if (rels.Ratings != null) {
            for (Rating rating : rels.Ratings) {
                map.Ratings.put(rating.Id, rating);
            }
        }
        if (rels.Users != null) {
            for (User user : rels.Users) {
                map.Users.put(user.Id, user);
            }
        }

        this.chatMessages(map.ChatMessages.values(), map);
        this.clients(map.Clients.values(), map);
        this.files(map.Files.values(), map);
        this.users(map.Users.values(), map);
        return map;
    }

    // Clients

    public void client(Client client, RelationMap map) {}

    public void clients(Collection<Client> clients, RelationMap map) {
        for (Client client : clients) {
            this.client(client, map);
        }
    }

    public void clientAuth(ClientAuth auth, RelationMap map) {
        this.client(auth.Client, map);
    }

    // Chat messages

    public void chatMessage(ChatMessage message, RelationMap map) {
        if (message.ClientId != null) {
            message.Client = map.Clients.get(message.ClientId);
        }
        if (message.UserId != null) {
            message.User = map.Users.get(message.UserId);
        }
        if (message.FileId != null) {
            message.File = map.Files.get(message.FileId);
        }
        if (message.RatingId != null) {
            message.Rating = map.Ratings.get(message.RatingId);
        }
        if (message.CreatedAt > 0) {
            message.Date = new Date(message.CreatedAt);
        }
    }

    public void chatMessages(Collection<ChatMessage> messages, RelationMap map) {
        for (ChatMessage message : messages) {
            this.chatMessage(message, map);
        }
    }

    // Chat events

    public void chatEvent(ChatEvent event, RelationMap map) {
        if (event.ClientId != null) {
            event.Client = map.Clients.get(event.ClientId);
        }
        if (event.UserId != null) {
            event.User = map.Users.get(event.UserId);
        }
        if (event.MessageId != null) {
            event.Message = map.ChatMessages.get(event.MessageId);
        }
    }

    public void chatEvents(Collection<ChatEvent> events, RelationMap map) {
        for (ChatEvent event : events) {
            this.chatEvent(event, map);
        }
    }

    // Files

    public void file(UploadedFile file, RelationMap map) {
        file.Url = this.fileUrl(file.Id);
        if (file.Type == FileType.IMAGE) {
            file.ImagePreviewUrl = this.fileImageUrl(file.Id, FileImageSize.PREVIEW);
            file.imageUrl = this.fileImageUrl(file.Id, FileImageSize.ORIGINAL);
        }
    }

    private String fileUrl(String fileId) {
        return String.format("%s/public/api/v1/files/get/%s", this.address, fileId);
    }

    private String fileImageUrl(String fileId, FileImageSize size) {
        return String.format("%s/public/api/v1/files/image/%s?size=%s", this.address, fileId, size);
    }

    public void files(Collection<UploadedFile> files, RelationMap map) {
        for (UploadedFile file : files) {
            this.file(file, map);
        }
    }

    // Users

    public void user(User user, RelationMap map) {
        if (user.AvatarId != null) {
            user.AvatarUrl = this.fileImageUrl(user.AvatarId, FileImageSize.AVATAR);
        }
    }

    public void users(Collection<User> users, RelationMap map) {
        for (User user : users) {
            this.user(user, map);
        }
    }
}
