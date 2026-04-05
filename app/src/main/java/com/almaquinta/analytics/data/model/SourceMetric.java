package com.almaquinta.analytics.data.model;

public class SourceMetric {
	private final String source;
	private final int activeUsers, newUsers;
	private final double engagementRate;

	public SourceMetric(String source, int activeUsers, int newUsers, double engagementRate) {
		this.source = source;
		this.activeUsers = activeUsers;
		this.newUsers = newUsers;
		this.engagementRate = engagementRate;
	}

	public String getSource() {
		return source;
	}

	public int getActiveUsers() {
		return activeUsers;
	}

	public int getNewUsers() {
		return newUsers;
	}

	public double getEngagementRate() {
		return engagementRate;
	}
}
