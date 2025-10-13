package com.ocean.scdemo.redis;

public class MnmRedisMessage<T> {

    private final String channel;
    private final String messagePublishedTime;
    private T payload;

    public MnmRedisMessage() {
        this.channel = "";
        this.messagePublishedTime = "";
    }

    public MnmRedisMessage(String channel, String messagePublishedTime, T payload) {
        this.channel = channel;
        this.messagePublishedTime = messagePublishedTime;
        this.payload = payload;
    }

}
