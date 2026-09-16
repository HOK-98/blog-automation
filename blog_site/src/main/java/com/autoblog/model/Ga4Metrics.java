package com.autoblog.model;

import java.util.List;

public class Ga4Metrics {
    private long views;
    private long users;
    private double averageSessionDuration;
    private double bounceRate;
    private List<ChannelMetric> channels;
    private List<DeviceMetric> devices;

    public long getViews() { return views; }
    public void setViews(long views) { this.views = views; }
    public long getUsers() { return users; }
    public void setUsers(long users) { this.users = users; }
    public double getAverageSessionDuration() { return averageSessionDuration; }
    public void setAverageSessionDuration(double averageSessionDuration) { this.averageSessionDuration = averageSessionDuration; }
    public double getBounceRate() { return bounceRate; }
    public void setBounceRate(double bounceRate) { this.bounceRate = bounceRate; }
    public List<ChannelMetric> getChannels() { return channels; }
    public void setChannels(List<ChannelMetric> channels) { this.channels = channels; }
    public List<DeviceMetric> getDevices() { return devices; }
    public void setDevices(List<DeviceMetric> devices) { this.devices = devices; }
}
