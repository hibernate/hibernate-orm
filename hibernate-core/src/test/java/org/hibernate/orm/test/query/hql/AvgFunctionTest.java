package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(annotatedClasses = AvgFunctionTest.Value.class)
@SessionFactory
public class AvgFunctionTest {

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction(
                session -> {
                    session.persist( new Value(0) );
                    session.persist( new Value(1) );
                    session.persist( new Value(2) );
                    session.persist( new Value(3) );
                    assertThat(
                            session.createQuery("select avg(value) from Value", Double.class)
                                    .getSingleResult(),
                            is(1.5)
                    );
                    assertThat(
                            session.createQuery("select avg(integerValue) from Value", Double.class)
                                    .getSingleResult(),
                            is(1.5)
                    );
                }
        );
    }

    @Entity(name="Value")
    public static class Value {
        public Value() {}
        public Value(int value) {
            this.value = value;
            this.integerValue = value;
        }
        @Id
        double value;
        int integerValue;
    }

}
