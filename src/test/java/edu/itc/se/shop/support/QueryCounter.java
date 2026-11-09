package edu.itc.se.shop.support;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManagerFactory;

@Component
public class QueryCounter {
    private final Statistics stats;

    public QueryCounter(EntityManagerFactory entityManagerFactory) {
        this.stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.stats.setStatisticsEnabled(true);
    }

    public void reset() {
        stats.clear();
    }

    public long count() {
        return stats.getPrepareStatementCount();
    }

    public long inserts() {
        return stats.getEntityInsertCount();
    }
}
