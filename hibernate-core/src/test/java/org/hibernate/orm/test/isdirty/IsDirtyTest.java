/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.isdirty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = {IsDirtyTest.X.class, IsDirtyTest.Y.class})
public class IsDirtyTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertFalse( session.isDirty() );
			X x = new X();
			session.persist( x );
			assertTrue( session.isDirty() );
			session.flush();
			assertFalse( session.isDirty() );
			x.i = 2;
			assertTrue( session.isDirty() );
			session.flush();
			assertFalse( session.isDirty() );
			session.remove( x );
			assertTrue( session.isDirty() );
			session.flush();
			assertFalse( session.isDirty() );
		} );
		scope.inTransaction( session -> {
			assertFalse( session.isDirty() );
			X x = new X();
			Y y = new Y();
			List<Y> ys = x.ys;
			x.ys.add( y );
			y.x = x;
			session.persist( x );
			assertTrue( session.isDirty() );
			session.flush();
			assertFalse( session.isDirty() );
			y.x = null;
			assertTrue( session.isDirty() );
			y.x = x;
			assertFalse( session.isDirty() );
			x.ys = null;
			assertTrue( session.isDirty() );
			x.ys = ys;
			assertFalse( session.isDirty() );
			ys.clear();
			assertTrue( session.isDirty() );
			ys.add( y );
			assertFalse( session.isDirty() );
			x.strings.add( "hello" );
			assertTrue( session.isDirty() );
			session.flush();
			assertFalse( session.isDirty() );
			x.strings.add( "world" );
			assertTrue( session.isDirty() );
			session.flush();
			assertFalse( session.isDirty() );
			x.strings.clear();
			assertTrue( session.isDirty() );
			session.flush();
			assertFalse( session.isDirty() );
			y.x = new X();
			assertTrue( session.isDirty() );
			y.x = x;
			assertFalse( session.isDirty() );
			ys.add( new Y() );
			assertTrue( session.isDirty() );
		} );
	}
	@Entity
	static class X {
		@Id @GeneratedValue
		Long id;
		int i = 1;
		@OneToMany(cascade = CascadeType.PERSIST)
		List<Y> ys = new ArrayList<>();
		@ElementCollection
		Set<String> strings = new HashSet<>();
	}
	@Entity
	static class Y {
		@Id @GeneratedValue
		Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		X x;
	}
}
