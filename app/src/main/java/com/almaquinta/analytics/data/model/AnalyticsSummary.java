package com.almaquinta.analytics.data.model;

public class AnalyticsSummary {
    private final int year;
    private final int month;
    private final int visits;
    private final int sessions;
    private final int activeUsers;
    private final int newUsers;
    private final double engagementRate;
    private final String topSource;

    public AnalyticsSummary(int year, int month, int visits, int sessions, int activeUsers, int newUsers, double engagementRate, String topSource) {
        this.year = year;
        this.month = month;
        this.visits = visits;
        this.sessions = sessions;
        this.activeUsers = activeUsers;
        this.newUsers = newUsers;
        this.engagementRate = engagementRate;
        this.topSource = topSource;
    }

    public int getYear() { return year; }

    public int getMonth() { return month; }

    public int getVisits() { return visits; }

    public int getSessions() { return sessions; }

    public int getActiveUsers() { return activeUsers; }

    public int getNewUsers() { return newUsers; }

    public double getEngagementRate() { return engagementRate; }

    public String getTopSource() { return topSource; }
}
