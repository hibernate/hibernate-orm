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
package org.hibernate.envers.test.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;
import org.hibernate.envers.tools.Pair;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class UpdatesPerformance extends AbstractPerformanceTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
        cfg.addAnnotatedClass(UnversionedStrTestEntity.class);
    }

    private final static int NUMBER_UPDATES = 1000;
    private final static int NUMBER_ENTITIES = 10;

    private Random random = new Random();

    private List<Integer> unversioned_ids = new ArrayList<Integer>();
    private List<Integer> versioned_ids = new ArrayList<Integer>();

    public void setup() {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        for (int i=0; i<NUMBER_ENTITIES; i++) {
            UnversionedStrTestEntity testEntity = new UnversionedStrTestEntity("x" + i);
            entityManager.persist(testEntity);
            unversioned_ids.add(testEntity.getId());
        }

        for (int i=0; i<NUMBER_ENTITIES; i++) {
            StrTestEntity testEntity = new StrTestEntity("x" + i);
            entityManager.persist(testEntity);
            versioned_ids.add(testEntity.getId());
        }
        entityManager.getTransaction().commit();
    }

    private void updateUnversioned() {
        EntityManager entityManager = getEntityManager();
        for (int i=0; i<NUMBER_UPDATES; i++) {
            entityManager.getTransaction().begin();
            Integer id = unversioned_ids.get(random.nextInt(NUMBER_ENTITIES));
            UnversionedStrTestEntity testEntity = entityManager.find(UnversionedStrTestEntity.class, id);
            testEntity.setStr("z" + i);
            entityManager.getTransaction().commit();
        }
    }

    private void updateVersioned() {
        EntityManager entityManager = getEntityManager();
        for (int i=0; i<NUMBER_UPDATES; i++) {
            entityManager.getTransaction().begin();
            Integer id = versioned_ids.get(random.nextInt(NUMBER_ENTITIES));
            StrTestEntity testEntity = entityManager.find(StrTestEntity.class, id);
            testEntity.setStr("z" + i);
            entityManager.getTransaction().commit();
        }
    }

    protected Pair<Long, Long> doTest() {
        long unversioned = measureTime(new Runnable() { public void run() { updateUnversioned(); } });
        long versioned = measureTime(new Runnable() { public void run() { updateVersioned(); } });

        return Pair.make(unversioned, versioned);
    }

    protected String getName() {
        return "UPDATES";
    }

    public static void main(String[] args) throws IOException {
        UpdatesPerformance updatesPerformance = new UpdatesPerformance();
        updatesPerformance.init();
        updatesPerformance.setup();
        updatesPerformance.run(10);
    }
}