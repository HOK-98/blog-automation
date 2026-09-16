package com.autoblog.model;

import java.util.List;

public class KeywordTrendData {
    private String keyword;
    private String startDate;
    private String endDate;
    private List<KeywordTrendPoint> points;
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
    public List<KeywordTrendPoint> getPoints() { return points; }
    public void setPoints(List<KeywordTrendPoint> points) { this.points = points; }

    public int getDemandScore() {
        if (points == null || points.isEmpty()) return 0;
        double average = getAverageRatio();
        double recent = getRecentAverageRatio();
        double trendBonus = Math.max(-15d, Math.min(20d, recent - average));
        int score = (int) Math.round((average * 0.65d) + (recent * 0.35d) + trendBonus);
        if (score < 0) return 0;
        if (score > 100) return 100;
        return score;
    }

    public double getAverageRatio() {
        if (points == null || points.isEmpty()) return 0d;
        double sum = 0d;
        for (KeywordTrendPoint point : points) {
            if (point != null) sum += point.getRatio();
        }
        return sum / points.size();
    }

    public double getRecentAverageRatio() {
        if (points == null || points.isEmpty()) return 0d;
        int start = Math.max(0, points.size() - 7);
        double sum = 0d;
        int count = 0;
        for (int i = start; i < points.size(); i++) {
            KeywordTrendPoint point = points.get(i);
            if (point != null) {
                sum += point.getRatio();
                count++;
            }
        }
        return count == 0 ? 0d : sum / count;
    }

    public String getTrendLabel() {
        if (points == null || points.size() < 7) return "데이터 부족";
        double average = getAverageRatio();
        double recent = getRecentAverageRatio();
        if (recent >= average * 1.15d && recent - average >= 5d) return "상승";
        if (recent <= average * 0.85d && average - recent >= 5d) return "하락";
        return "안정";
    }

    public String getDemandLabel() {
        int score = getDemandScore();
        if (score >= 70) return "우선 작성";
        if (score >= 45) return "작성 가능";
        if (score >= 20) return "보류";
        return "데이터 부족";
    }
}
