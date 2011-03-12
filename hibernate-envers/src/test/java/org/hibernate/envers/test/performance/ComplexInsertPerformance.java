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
import java.util.Date;
import java.util.HashSet;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.performance.complex.RootEntity;
import org.hibernate.envers.test.performance.complex.ChildEntity2;
import org.hibernate.envers.test.performance.complex.ChildEntity1;

import org.hibernate.ejb.Ejb3Configuration;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ComplexInsertPerformance extends AbstractPerformanceTest {
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(RootEntity.class);
        cfg.addAnnotatedClass(ChildEntity1.class);
        cfg.addAnnotatedClass(ChildEntity2.class);
    }

    private final static int NUMBER_INSERTS = 1000;

    private long idCounter = 0;

    private ChildEntity2 createChildEntity2() {
        ChildEntity2 ce = new ChildEntity2();
        ce.setId(idCounter++);
        ce.setNumber(12345678);
        ce.setData("some data, not really meaningful");
        ce.setStrings(new HashSet<String>());
        ce.getStrings().add("aaa");
        ce.getStrings().add("bbb");
        ce.getStrings().add("ccc");

        return ce;
    }

    private ChildEntity1 createChildEntity1() {
        ChildEntity1 ce = new ChildEntity1();
        ce.setId(idCounter++);
        ce.setData1("xxx");
        ce.setData2("yyy");
        ce.setChild1(createChildEntity2());
        ce.setChild2(createChildEntity2());

        return ce;
    }

    protected void doTest() {
        for (int i=0; i<NUMBER_INSERTS; i++) {
            newEntityManager();
            EntityManager entityManager = getEntityManager();

            entityManager.getTransaction().begin();

            RootEntity re = new RootEntity();
            re.setId(idCounter++);
            re.setData1("data1");
            re.setData2("data2");
            re.setDate1(new Date());
            re.setNumber1(123);
            re.setNumber2(456);
            re.setChild1(createChildEntity1());
            re.setChild2(createChildEntity1());
            re.setChild3(createChildEntity1());

            start();
            entityManager.persist(re);            
            entityManager.getTransaction().commit();
            stop();
        }
    }

    public static void main(String[] args) throws IOException {
        ComplexInsertPerformance insertsPerformance = new ComplexInsertPerformance();
        insertsPerformance.test(3);
    }
}
