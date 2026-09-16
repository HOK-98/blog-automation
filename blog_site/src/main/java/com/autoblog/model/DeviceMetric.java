package com.autoblog.model;

public class DeviceMetric {
    private String name;
    private long users;

    public DeviceMetric() {}

    public DeviceMetric(String name, long users) {
        this.name = name;
        this.users = users;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getUsers() { return users; }
    public void setUsers(long users) { this.users = users; }
}
