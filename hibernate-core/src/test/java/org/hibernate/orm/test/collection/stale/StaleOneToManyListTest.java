/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.stale;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.cfg.BatchSettings.STATEMENT_BATCH_SIZE;

@Jpa(annotatedClasses = {StaleOneToManyListTest.StaleListTestParent.class,
						StaleOneToManyListTest.StaleListTestChild.class},
		properties = @Setting(name = STATEMENT_BATCH_SIZE, value = "5"))
@SkipForDialect(dialectClass = MariaDBDialect.class)
@SkipForDialect(dialectClass = CockroachDialect.class, reason = "CockroachDB uses SERIALIZABLE isolation, and does not support this")
public class StaleOneToManyListTest {
	@Test void test1(EntityManagerFactoryScope scope) {
		var entity = new StaleListTestParent();
		entity.childList.add( new StaleListTestChild("hello") );
		entity.childList.add( new StaleListTestChild("world") );
		scope.inTransaction( session -> {
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			var e = session.find( StaleListTestParent.class, entity.id );
			e.childList.set( 0, new StaleListTestChild("goodbye") );
			scope.inTransaction( session2 -> {
				var e2 = session2.find( StaleListTestParent.class, entity.id );
				e2.childList.set( 0, new StaleListTestChild("BYE") );
			} );
		} );
	}
	@Test void test2(EntityManagerFactoryScope scope) {
		var entity = new StaleListTestParent();
		entity.childList.add( new StaleListTestChild("hello") );
		entity.childList.add( new StaleListTestChild("world") );
		scope.inTransaction( session -> {
			session.persist( entity );
		} );
		scope.inTransaction( session -> {
			var e = session.find( StaleListTestParent.class, entity.id );
			e.childList.set( 1, new StaleListTestChild("everyone") );
			scope.inTransaction( session2 -> {
				var e2 = session2.find( StaleListTestParent.class, entity.id );
				e2.childList.remove( 1 );
			} );
		} );
	}
	@FailureExpected(reason = "ConstraintViolationException")
	@Test void test3(EntityManagerFactoryScope scope) {
		var parent1 = new StaleListTestParent();
		var parent2 = new StaleListTestParent();
		var child1 = new StaleListTestChild( "hello" );
		var child2 = new StaleListTestChild( "world" );
		parent1.childList.add( child1 );
		parent1.childList.add( child2 );
		scope.inTransaction( session -> {
			session.persist( parent1 );
			session.persist( parent2 );
		} );
		scope.inTransaction( session1 -> {
			var p = session1.find( StaleListTestParent.class, parent1.id );
			var c = session1.find( StaleListTestChild.class, child1.id );
			p.childList.remove( c );
			scope.inTransaction( session2 -> {
				var pp = session2.find( StaleListTestParent.class, parent2.id );
				var cc = session2.find( StaleListTestChild.class, child1.id );
				pp.childList.add( cc );
			} );
		} );
	}
	@Test void test4(EntityManagerFactoryScope scope) {
		var parent1 = new StaleListTestParent();
		var parent2 = new StaleListTestParent();
		var child1 = new StaleListTestChild( "hello" );
		var child2 = new StaleListTestChild( "world" );
		parent1.childList.add( child1 );
		parent1.childList.add( child2 );
		scope.inTransaction( session -> {
			session.persist( parent1 );
			session.persist( parent2 );
		} );
		scope.inTransaction( session1 -> {
			var p = session1.find( StaleListTestParent.class, parent1.id );
			var c = session1.find( StaleListTestChild.class, child1.id );
			p.childList.remove( c );
		} );
		scope.inTransaction( session2 -> {
			var pp = session2.find( StaleListTestParent.class, parent2.id );
			var cc = session2.find( StaleListTestChild.class, child1.id );
			pp.childList.add( cc );
		} );
	}
	static @Entity(name = "StaleParent") class StaleListTestParent {
		@GeneratedValue @Id
		long id;
		@OneToMany(cascade = CascadeType.PERSIST) @OrderColumn
		List<StaleListTestChild> childList = new ArrayList<>();
	}
	static @Entity(name = "StaleChild") class StaleListTestChild {
		@GeneratedValue @Id
		long id;
		String text;
		StaleListTestChild(String text) {
			this.text = text;
		}
		StaleListTestChild() {
		}
	}
}
