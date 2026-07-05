/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.EntityFilterException;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SessionFactory
@DomainModel(annotatedClasses =
		{ManyToOneFilterTest.X.class,
				ManyToOneFilterTest.Y.class,
				ManyToOneFilterTest.Z.class})
@JiraKey("HHH-19566")
class ManyToOneFilterTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Y y = new Y();
			Z z = new Z();
			X x = new X();
			x.id = -1;
			y.x = x;
			z.xs.add( x );
			session.persist(x);
			session.persist(y);
			session.persist(z);
		});
		scope.inTransaction(session -> {
			Y y = session.find(Y.class, 0L);
			assertNotNull(y.x);
			Z z = session.find(Z.class, 0L);
			assertEquals( 1, z.xs.size() );
		});
		scope.inTransaction( session -> {
			session.enableFilter( "filter" ).validate();
			Z z = session.find(Z.class, 0L);
			assertEquals( 0, z.xs.size() );
		} );
		assertThrows( EntityFilterException.class, () ->
				scope.inTransaction( session -> {
					session.enableFilter( "filter" ).validate();
					var graph = session.createEntityGraph(Y.class);
					session.find( graph, 0L );
				} )
		);
		assertThrows( EntityFilterException.class, () ->
				scope.inTransaction(session -> {
					session.enableFilter( "filter" ).validate();
					session.find(Y.class, 0L);
				})
		);
	}

	@Entity
	@Table(name = "XX")
	@FilterDef(name = "filter", applyToLoadByKey = true)
	@Filter(name = "filter", condition = "id>0")
	static class X {
		@Id
		long id;
	}
	@Entity
	@Table(name = "YY")
	static class Y {
		@Id
		long id;
		String name;
		@ManyToOne
		@JoinColumn(name = "xx")
		X x;
	}
	@Entity
	@Table(name = "ZZ")
	static class Z {
		@Id
		long id;
		@OneToMany(fetch = FetchType.EAGER)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(name = "zz")
		@Filter(name = "filter", condition = "id>0")
		Set<X> xs = new HashSet<>();
	}
}
