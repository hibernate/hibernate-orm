/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql;

import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel
public class BooleanPredicateTest {
    @Test void test(SessionFactoryScope scope) {
        scope.inSession(session -> {
            assertEquals(1, session.createSelectionQuery("select 1 where true is true").getResultList().size());
            assertEquals(0, session.createSelectionQuery("select 1 where false is true").getResultList().size());
            assertEquals(0, session.createSelectionQuery("select 1 where true is not true").getResultList().size());
            assertEquals(1, session.createSelectionQuery("select 1 where false is not true").getResultList().size());

            assertEquals(0, session.createSelectionQuery("select 1 where true is false").getResultList().size());
            assertEquals(1, session.createSelectionQuery("select 1 where false is false").getResultList().size());
            assertEquals(1, session.createSelectionQuery("select 1 where true is not false").getResultList().size());
            assertEquals(0, session.createSelectionQuery("select 1 where false is not false").getResultList().size());
        });
    }

    @SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby doesn't accept literal null in case")
    @Test void testNulls(SessionFactoryScope scope) {
        scope.inSession(session -> {
            assertEquals(1, session.createSelectionQuery("select 1 where true is true").getResultList().size());
            assertEquals(0, session.createSelectionQuery("select 1 where false is true").getResultList().size());
            assertEquals(0, session.createSelectionQuery("select 1 where null is true").getResultList().size());
            assertEquals(0, session.createSelectionQuery("select 1 where true is not true").getResultList().size());
            assertEquals(1, session.createSelectionQuery("select 1 where false is not true").getResultList().size());
            assertEquals(1,  session.createSelectionQuery("select 1 where null is not true").getResultList().size());

            assertEquals(0, session.createSelectionQuery("select 1 where true is false").getResultList().size());
            assertEquals(1, session.createSelectionQuery("select 1 where false is false").getResultList().size());
            assertEquals(0, session.createSelectionQuery("select 1 where null is false").getResultList().size());
            assertEquals(1, session.createSelectionQuery("select 1 where true is not false").getResultList().size());
            assertEquals(0, session.createSelectionQuery("select 1 where false is not false").getResultList().size());
            assertEquals(1,  session.createSelectionQuery("select 1 where null is not false").getResultList().size());
        });
    }
}
