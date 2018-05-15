/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 *
 */
public class CountEntityWithCompositeIdTest extends BaseEntityManagerFunctionalTestCase {

    @Test
    public void shouldCount() {
        EntityManager em = getOrCreateEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<EntityWithCompositeId> r = cq.from(EntityWithCompositeId.class);
        cq.multiselect(cb.count(r));
        assertThat(em.createQuery(cq).getSingleResult().intValue(), is(0));
    }

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] {EntityWithCompositeId.class, CompositeId.class};
    }
}
