package org.hibernate.orm.test.query;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-16721")
@DomainModel(annotatedClasses = {
        ImplicitJoinInSubqueryTest.A.class,
        ImplicitJoinInSubqueryTest.B.class,
        ImplicitJoinInSubqueryTest.C.class
})
@SessionFactory(useCollectingStatementInspector = true)
public class ImplicitJoinInSubqueryTest {

    @Test
    public void testImplicitJoinInSubquery(SessionFactoryScope scope) {
        SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
        statementInspector.clear();
        scope.inTransaction(
                entityManager -> {
                    entityManager.createSelectionQuery(
                            "select 1 from A a where exists (select 1 from B b where a.b.c.id = 5)"
                    ).getResultList();
                    assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "b2_0.id=a1_0.b_id" );
                }
        );
    }

    @Entity(name = "A")
    public static class A {
        @Id
        @GeneratedValue
        Long id;
        @ManyToOne(fetch = FetchType.LAZY)
        B b;
    }

    @Entity(name = "B")
    public static class B {
        @Id
        @GeneratedValue
        Long id;
        @ManyToOne(fetch = FetchType.LAZY)
        C c;
    }

    @Entity(name = "C")
    public static class C {
        @Id
        @GeneratedValue
        Long id;
    }
}
