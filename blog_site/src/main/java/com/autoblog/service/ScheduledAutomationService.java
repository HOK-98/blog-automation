package com.autoblog.service;

public interface ScheduledAutomationService {
    void start();
    void stop();
    void runNow();
    boolean runNowAsync();
}
