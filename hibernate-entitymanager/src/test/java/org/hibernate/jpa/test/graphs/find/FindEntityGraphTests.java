/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.jpa.test.graphs.find;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Christian Bauer
 */
public class FindEntityGraphTests extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[]{Foo.class, Bar.class, Baz.class};
    }

    @Test
    public void loadParallelManyToOne() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();

        Bar bar = new Bar();
        bar.id = 1;
        bar.name = "bar";
        em.persist(bar);

        Baz baz = new Baz();
        baz.id = 2;
        baz.name = "baz";
        em.persist(baz);

        Foo foo = new Foo();
        foo.id = 3;
        foo.name = "foo";
        foo.bar = bar;
        foo.baz = baz;
        em.persist(foo);

        em.getTransaction().commit();
        em.close();

        em = getOrCreateEntityManager();
        em.getTransaction().begin();

        EntityGraph<Foo> fooGraph = em.createEntityGraph(Foo.class);
        fooGraph.addAttributeNodes("bar", "baz");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("javax.persistence.loadgraph", fooGraph);

        Foo result = em.find(Foo.class, foo.id, properties);

        assertTrue(Hibernate.isInitialized(result));
        assertTrue(Hibernate.isInitialized(result.bar));
        assertTrue(Hibernate.isInitialized(result.baz));

        em.getTransaction().commit();
        em.close();
    }

    @Entity
    public static class Foo {

        @Id
        public Integer id;

        public String name;

        @ManyToOne(fetch = FetchType.LAZY)
        public Bar bar;

        @ManyToOne(fetch = FetchType.LAZY)
        public Baz baz;
    }

    @Entity
    public static class Bar {

        @Id
        public Integer id;

        public String name;
    }

    @Entity
    public static class Baz {

        @Id
        public Integer id;

        public String name;
    }

}
