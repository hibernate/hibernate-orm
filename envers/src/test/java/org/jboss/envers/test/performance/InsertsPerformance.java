/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.jboss.envers.test.performance;

import java.io.IOException;
import javax.persistence.EntityManager;

import org.jboss.envers.test.entities.StrTestEntity;
import org.jboss.envers.test.entities.UnversionedStrTestEntity;
import org.jboss.envers.tools.Pair;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class InsertsPerformance extends AbstractPerformanceTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(UnversionedStrTestEntity.class);
    }

    private final static int NUMBER_INSERTS = 1000;
    
    private void insertUnversioned() {
        EntityManager entityManager = getEntityManager();
        for (int i=0; i<NUMBER_INSERTS; i++) {
            entityManager.getTransaction().begin();
            entityManager.persist(new UnversionedStrTestEntity("x" + i));
            entityManager.getTransaction().commit();
        }
    }

    private void insertVersioned() {
        EntityManager entityManager = getEntityManager();
        for (int i=0; i<NUMBER_INSERTS; i++) {
            entityManager.getTransaction().begin();
            entityManager.persist(new StrTestEntity("x" + i));
            entityManager.getTransaction().commit();
        }
    }

    protected Pair<Long, Long> doTest() {
        long unversioned = measureTime(new Runnable() { public void run() { insertUnversioned(); } });
        long versioned = measureTime(new Runnable() { public void run() { insertVersioned(); } });

        return Pair.make(unversioned, versioned);
    }

    protected String getName() {
        return "INSERTS";
    }

    public static void main(String[] args) throws IOException {
        InsertsPerformance insertsPerformance = new InsertsPerformance();
        insertsPerformance.init();
        insertsPerformance.run(10);
    }
}
