/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs.inherited;

import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.Subgraph;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertTrue;

/**
 * @author Oliver Breidenbach
 */
public class InheritedEntityGraphTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Foo2.class, Foo.class, Bar.class };
	}

    @Test
    @TestForIssue(jiraKey = "HHH-10261")
    public void singleAttributeNodeInheritanceTest() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();

        Bar bar = new Bar();
        em.persist(bar);

        Foo foo = new Foo();
        foo.bar = bar;
        em.persist( foo );

        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();

        EntityGraph<Foo> entityGraph = em.createEntityGraph( Foo.class );
        entityGraph.addSubgraph( "bar" );

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "javax.persistence.loadgraph", entityGraph );

        Foo result = em.find( Foo.class, foo.id, properties );

        assertTrue( Hibernate.isInitialized( result ) );
        assertTrue( Hibernate.isInitialized( result.bar ) );

        em.getTransaction().commit();
        em.close();
    }

    @Test
    @TestForIssue(jiraKey = "HHH-10261")
    public void collectionAttributeNodeInheritanceTest() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();

        Bar bar = new Bar();
        em.persist(bar);

        Foo foo = new Foo();
        foo.bar = bar;
        em.persist( foo );

        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();

        EntityGraph<Foo> entityGraph = em.createEntityGraph( Foo.class );
        entityGraph.addSubgraph( "bars" );

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "javax.persistence.loadgraph", entityGraph );

        Foo result = em.find( Foo.class, foo.id, properties );

        assertTrue( Hibernate.isInitialized( result ) );
        assertTrue( Hibernate.isInitialized( result.bars ) );

        em.getTransaction().commit();
        em.close();
    }


    @Test
    @TestForIssue(jiraKey = "HHH-10261")
    public void singleAttributeSubgraphInheritanceTest() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();

        Bar bar = new Bar();
        em.persist(bar);

        Foo foo = new Foo();
        foo.bar = bar;
        em.persist( foo );

        Foo2 foo2 = new Foo2();
        foo2.foo = foo;
        em.persist( foo2 );

        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();

        EntityGraph<Foo2> entityGraph = em.createEntityGraph( Foo2.class );
        Subgraph<Foo> subgraphFoo = entityGraph.addSubgraph( "foo" );
        subgraphFoo.addSubgraph( "bar" );

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "javax.persistence.loadgraph", entityGraph );

        Foo2 result = em.find( Foo2.class, foo2.id, properties );

        assertTrue( Hibernate.isInitialized( result ) );
        assertTrue( Hibernate.isInitialized( result.foo ) );
        assertTrue( Hibernate.isInitialized( result.foo.bar ) );

        em.getTransaction().commit();
        em.close();
    }

    @Test
    @TestForIssue(jiraKey = "HHH-10261")
    public void collectionAttributeSubgraphInheritanceTest() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();

        Bar bar = new Bar();
        em.persist(bar);

        Foo foo = new Foo();
        foo.bar = bar;
        em.persist( foo );

        Foo2 foo2 = new Foo2();
        foo2.foo = foo;
        em.persist( foo2 );

        em.getTransaction().commit();
        em.clear();

        em.getTransaction().begin();

        EntityGraph<Foo2> entityGraph = em.createEntityGraph( Foo2.class );
        Subgraph<Foo> subgraphFoo = entityGraph.addSubgraph( "foo" );
        subgraphFoo.addSubgraph( "bars" );

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put( "javax.persistence.loadgraph", entityGraph );

        Foo2 result = em.find( Foo2.class, foo2.id, properties );

        assertTrue( Hibernate.isInitialized( result ) );
        assertTrue( Hibernate.isInitialized( result.foo ) );
        assertTrue( Hibernate.isInitialized( result.foo.bars ) );

        em.getTransaction().commit();
        em.close();
    }
}
