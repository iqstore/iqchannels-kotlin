package ru.iqchannels.sdk.app;

public class IQChannelsConfig {
    public String address;
    public String channel;

    public IQChannelsConfig() {}

    public IQChannelsConfig(String address, String channel) {
        this.address = address;
        this.channel = channel;
    }
}
