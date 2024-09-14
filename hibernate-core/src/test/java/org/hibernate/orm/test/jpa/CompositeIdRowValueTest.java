/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@JiraKey( value = "HHH-9029")
@Jpa(annotatedClasses = {
        EntityWithCompositeId.class,
        CompositeId.class
})
public class CompositeIdRowValueTest {

    @Test
    public void testTupleAfterSubQuery(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    Query q = entityManager.createQuery("SELECT e FROM EntityWithCompositeId e "
                                                     + "WHERE EXISTS (SELECT 1 FROM EntityWithCompositeId) "
                                                     + "AND e.id = :id");

                    q.setParameter("id", new CompositeId(1, 2));

                    assertThat(q.getResultList().size(), is(0));
                }
        );
    }
}
