package ru.iqchannels.sdk.schema;


import androidx.annotation.Nullable;

/**
 * Created by Ivan Korobkov i.korobkov@iqstore.ru on 26/01/2017.
 */
public class UploadedFile {
    public String Id;
    public FileType Type;

    public FileOwnerType Owner;
    @Nullable public Long OwnerClientId;

    public ActorType Actor;
    @Nullable public Long ActorClientId;
    @Nullable public Long ActorUserId;

    public String Name; // Original file name.
    public String Path; // Relative filesystem path;
    public long Size; // Size in bytes.

    @Nullable public Integer ImageWidth;
    @Nullable public Integer ImageHeight;

    public String ContentType;
    public long CreatedAt;

    // Local
    @Nullable public String Url;
    @Nullable public String ImagePreviewUrl;
    @Nullable public String imageUrl;
}
