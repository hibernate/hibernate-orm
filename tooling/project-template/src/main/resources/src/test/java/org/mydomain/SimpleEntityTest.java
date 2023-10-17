/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.mydomain;

import junit.framework.TestCase;
import org.mydomain.SimpleEntity;
import org.junit.Test;
import org.junit.Assert;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

public class SimpleEntityTest extends TestCase {
    @Test
    public void testSimpleEntity() {
        EntityManagerFactory factory = Persistence.createEntityManagerFactory("h2-example");
        EntityManager em = factory.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();
        SimpleEntity s1 = new SimpleEntity(1, "One");
        SimpleEntity s2 = new SimpleEntity(2, "Two");
        em.persist(s1);
        em.persist(s2);

        tx.commit();
        em.close();

        em = factory.createEntityManager();
        tx = em.getTransaction();
        tx.begin();
        SimpleEntity one = em.find(SimpleEntity.class, 1);
        Assert.assertEquals("One", one.getName());
        tx.commit();
        em.close();
    }
}
