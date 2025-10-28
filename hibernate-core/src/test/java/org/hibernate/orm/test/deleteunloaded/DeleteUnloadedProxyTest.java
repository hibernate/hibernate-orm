/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deleteunloaded;

import org.hibernate.Transaction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel( annotatedClasses = { Parent.class, Child.class, ParentSub.class } )
@SessionFactory
//@ServiceRegistry(
//        settings = {
//                @Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "0")
//        }
//)
public class DeleteUnloadedProxyTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
	@Test
	public void testAttached(SessionFactoryScope scope) {
		Parent p = new Parent();
		Child c = new Child();
		scope.inSession( em -> {
			Transaction tx = em.beginTransaction();
			c.setParent(p);
			p.getChildren().add(c);
			p.getWords().add("hello");
			p.getWords().add("world");
			em.persist(p);
			tx.commit();
		} );
		scope.inSession( em -> {
			Transaction tx = em.beginTransaction();
			Child child = em.getReference( Child.class, c.getId() );
			assertFalse( isInitialized(child) );
			em.remove(child);
			Parent parent = em.getReference( Parent.class, p.getId() );
			assertFalse( isInitialized(parent) );
			em.remove(parent);
			tx.commit();
			assertFalse( isInitialized(child) );
			assertFalse( isInitialized(parent) );
		} );
		scope.inSession( em -> {
			assertNull( em.find( Parent.class, p.getId() ) );
			assertNull( em.find( Child.class, c.getId() ) );
		} );
	}
	@Test
	public void testDetached(SessionFactoryScope scope) {
		Parent p = new Parent();
		Child c = new Child();
		scope.inSession( em -> {
			Transaction tx = em.beginTransaction();
			c.setParent(p);
			p.getChildren().add(c);
			p.getWords().add("hello");
			p.getWords().add("world");
			em.persist(p);
			tx.commit();
		} );
		Child cc = scope.fromSession( em -> {
			Transaction tx = em.beginTransaction();
			Child child = em.getReference( Child.class, c.getId() );
			assertFalse( isInitialized(child) );
			return child;
		} );
		Parent pp = scope.fromSession( em -> {
			Transaction tx = em.beginTransaction();
			Parent parent = em.getReference( Parent.class, p.getId() );
			assertFalse( isInitialized(parent) );
			return parent;
		} );
		scope.inSession( em -> {
			Transaction tx = em.beginTransaction();
			em.remove(cc);
			em.remove(pp);
			tx.commit();
			assertFalse( isInitialized(cc) );
			assertFalse( isInitialized(pp) );
		} );
		scope.inSession( em -> {
			assertNull( em.find( Parent.class, p.getId() ) );
			assertNull( em.find( Child.class, c.getId() ) );
		} );
	}

	@Test
	@JiraKey( "HHH-16690" )
	public void testRePersist(SessionFactoryScope scope) {
		Parent p = new Parent();
		ParentSub ps = new ParentSub( 1L, "abc", p );
		scope.inTransaction( em -> {
			em.persist( p );
			em.persist( ps );
		} );
		scope.inTransaction( em -> {
			ParentSub sub = em.getReference( ParentSub.class, 1L );
			assertFalse( isInitialized( sub ) );
			em.remove( sub );
			em.persist( new ParentSub( 1L, "def", p ) );
		} );
		scope.inSession( em -> {
			ParentSub sub = em.find( ParentSub.class, 1L );
			assertNotNull( sub );
			assertEquals( "def", sub.getData() );
		} );
	}

	@Test
	@JiraKey( "HHH-16690" )
	public void testReMerge(SessionFactoryScope scope) {
		Parent p = new Parent();
		ParentSub ps = new ParentSub( 1L, "abc", p );
		scope.inTransaction( em -> {
			em.persist( p );
			em.persist( ps );
		} );
		scope.inTransaction( em -> {
			ParentSub sub = em.getReference( ParentSub.class, 1L );
			assertFalse( isInitialized( sub ) );
			em.remove( sub );
			em.merge( new ParentSub( 1L, "def", p ) );
		} );
		scope.inSession( em -> {
			ParentSub sub = em.find( ParentSub.class, 1L );
			assertNotNull( sub );
			assertEquals( "def", sub.getData() );
		} );
	}
}
