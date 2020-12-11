package org.hibernate.test.preparedStatement;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.persistence.*;
import javax.persistence.criteria.*;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.inline.InlineIdsOrClauseBulkIdStrategy;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.stat.Statistics;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue( jiraKey = "HHH-14365" )
public class PreparedStatementCountTest extends BaseEntityManagerFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                Person.class
        };
    }

    @Test
    public void testPreparedStatementCount() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            SessionFactory sessionFactory = this.entityManagerFactory().unwrap(SessionFactory.class);
            Statistics statistics = sessionFactory.getStatistics();
            statistics.setStatisticsEnabled(true);

            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<String> select = builder.createQuery(String.class);

            Root<Person> root = select.from(Person.class);
            select.select(root.get("name"));

            ParameterExpression<Long> peId = builder.parameter(Long.class, "i");
            Predicate p1 = builder.equal(root.get("id"), peId);

            select.where(p1);

            List<String> results = Collections.emptyList();
            TypedQuery<String> query = entityManager.createQuery(select);

            for (int i = 0;i < 30;i++) {
                query.setParameter(peId, 1L);
                results = query.getResultList();
            }

            System.err.println("prepared statement count: " + statistics.getPrepareStatementCount());
            System.err.println("query cache hit count: " + statistics.getQueryCacheHitCount());
            System.err.println("query cache miss count: " + statistics.getQueryCacheMissCount());
            System.err.println("query cache execution count: " + statistics.getQueryExecutionCount());

            System.err.println("results: " + results);

            assertEquals(1, statistics.getPrepareStatementCount());
        });
    }

    @Entity(name = "Person")
    @Inheritance(strategy = InheritanceType.JOINED)
    public static class Person {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        private boolean employed;

        @Temporal( TemporalType.TIMESTAMP )
        private Date employedOn;

        //Getters and setters omitted for brevity
    }
}
