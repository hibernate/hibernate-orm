/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(annotatedClasses = AvgFunctionTest.Score.class)
@SessionFactory
public class AvgFunctionTest {

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction(
                session -> {
                    session.persist( new Score(0) );
                    session.persist( new Score(1) );
                    session.persist( new Score(2) );
                    session.persist( new Score(3) );
                    assertThat(
                            session.createQuery("select avg(doubleValue) from ScoreForAvg", Double.class)
                                    .getSingleResult(),
                            is(1.5)
                    );
                    assertThat(
                            session.createQuery("select avg(integerValue) from ScoreForAvg", Double.class)
                                    .getSingleResult(),
                            is(1.5)
                    );
                }
        );
    }

    @Entity(name="ScoreForAvg")
    public static class Score {
        public Score() {}
        public Score(int value) {
            this.doubleValue = value;
            this.integerValue = value;
        }
        @Id
        double doubleValue;
        int integerValue;
    }

}
