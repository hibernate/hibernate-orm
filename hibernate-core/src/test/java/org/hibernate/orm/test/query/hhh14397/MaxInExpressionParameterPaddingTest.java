package org.hibernate.orm.test.query.hhh14397;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.jdbc.DefaultSQLStatementInspectorSettingProvider;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Nivruth N
 */
@TestForIssue(jiraKey = "HHH-14397")
@RequiresDialect(SQLServerDialect.class)
@Jpa(
        annotatedClasses = {org.hibernate.orm.test.query.MaxInExpressionParameterPaddingTest.Person.class},
        integrationSettings = {
                @Setting(name = AvailableSettings.USE_SQL_COMMENTS, value = "true"),
                @Setting(name = AvailableSettings.IN_CLAUSE_PARAMETER_PADDING, value = "true"),
        },
        settingProviders = {
                @SettingProvider(
                        settingName = AvailableSettings.DIALECT,
                        provider = org.hibernate.orm.test.query.MaxInExpressionParameterPaddingTest.DialectProvider.class
                ),
                @SettingProvider(
                        settingName = AvailableSettings.STATEMENT_INSPECTOR,
                        provider = DefaultSQLStatementInspectorSettingProvider.class
                )
        }
)
public class MaxInExpressionParameterPaddingTest {

    public static class DialectProvider implements SettingProvider.Provider<String> {
        @Override
        public String getSetting() {
            return MaxInExpressionParameterPaddingTest.MaxCountInExpressionSQLServerDialect.class.getName();
        }
    }

    public static final int MAX_COUNT = 65532;

    @BeforeAll
    protected void afterEntityManagerFactoryBuilt(EntityManagerFactoryScope scope) {
        scope.inTransaction(entityManager -> {
                    for (int i = 0; i < MAX_COUNT; i++) {
                        org.hibernate.orm.test.query.MaxInExpressionParameterPaddingTest.Person person =
                                new org.hibernate.orm.test.query.MaxInExpressionParameterPaddingTest.Person();
                        person.setId(i);
                        person.setName(String.format("Person nr %d", i));
                        entityManager.persist(person);
                    }
                }
        );
    }

    @Test
    public void testInClauseParameterPadding(final EntityManagerFactoryScope scope) {
        final SQLStatementInspector statementInspector = scope.getStatementInspector( SQLStatementInspector.class );
        statementInspector.clear();

        scope.inTransaction( entityManager ->
                entityManager.createQuery( "select p from Person p where p.id in :ids" )
                        .setParameter( "ids", IntStream.range( 0, MAX_COUNT ).boxed()
                                .collect( Collectors.toList() ) )
                        .getResultList()
        );

        final String expectedInClause = "in(?" +
                ",?".repeat(MAX_COUNT - 1) +
                ")";

        assertTrue( statementInspector.getSqlQueries().get( 0 ).endsWith(expectedInClause) );
    }

    @Test
    public void testInClauseParameterPaddingOutOfBounds(final EntityManagerFactoryScope scope) {
        final SQLStatementInspector statementInspector = scope.getStatementInspector( SQLStatementInspector.class );
        statementInspector.clear();

        scope.inTransaction( entityManager ->
                entityManager.createQuery( "select p from Person p where p.id in :ids" )
                        .setParameter( "ids", IntStream.range( 0, MAX_COUNT * 2 ).boxed()
                                .collect( Collectors.toList() ) )
                        .getResultList()
        );

        final String expectedInClause = "in(?" +
                ",?".repeat(MAX_COUNT - 1) +
                ")" +
                " or p1_0.id in(?" +
                ",?".repeat(MAX_COUNT - 1);

        assertTrue( statementInspector.getSqlQueries().get( 0 ).endsWith(expectedInClause) );
    }

    @Entity(name = "Person")
    public static class Person {

        @Id
        private Integer id;

        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class MaxCountInExpressionSQLServerDialect extends SQLServerDialect {

        public MaxCountInExpressionSQLServerDialect() {
        }

        @Override
        public int getInExpressionCountLimit() {
            return MAX_COUNT;
        }
    }

}
