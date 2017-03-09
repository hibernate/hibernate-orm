package org.hibernate.test.cfg;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class ColumnSequenceMatchesEntityFieldsTest extends BaseEntityManagerFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{ExampleEntity.class};
    }

    @Test
    @TestForIssue(jiraKey = "HHH-11386")
    public void testColumnOrderMatchesAttributeOrder() throws SQLException {
        AbstractEntityPersister entityPersister = (AbstractEntityPersister) entityManagerFactory().getMetamodel().entityPersister(ExampleEntity.class);
        String second = entityPersister.getPropertyColumnNames(0)[0];
        String first = entityPersister.getPropertyColumnNames(1)[0];
        String third = entityPersister.getPropertyColumnNames(2)[0];
        assertEquals("second", second);
        assertEquals("first", first);
        assertEquals("third", third);
    }

    @Entity
    @Table(name = "example")
    private static class ExampleEntity {
        @Id
        private Integer id;

        @Column
        private Integer second;

        @Column
        private Integer first;

        @Column
        private Integer third;
    }
}