package ru.iqchannels.sdk.app;

import ru.iqchannels.sdk.Log;

public class IQChannelsConfig {
    public String address;
    public String channel;

    public IQChannelsConfig() {}

    public IQChannelsConfig(String address, String channel) {
        this(address, channel, true);
    }

    public IQChannelsConfig(String address, String channel, boolean logging) {
        this.address = address;
        this.channel = channel;
        Log.configure(logging);
    }
}
