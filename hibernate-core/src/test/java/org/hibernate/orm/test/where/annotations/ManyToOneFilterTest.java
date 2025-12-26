/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.EntityFilterException;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses =
		{ManyToOneFilterTest.X.class,
		ManyToOneFilterTest.Y.class})
class ManyToOneFilterTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Y y = new Y();
			X x = new X();
			x.id = -1;
			y.x = x;
			session.persist(x);
			session.persist(y);
		});
		scope.inTransaction(session -> {
			Y y = session.find(Y.class, 0L);
			assertNotNull(y.x);
		});
		try {
			scope.inTransaction( session -> {
				session.enableFilter( "filter" ).validate();
				var graph = session.createEntityGraph(Y.class);
//			graph.removeAttributeNode(ManyToOneFilterTest_.Y_.x);
				Y y = session.find( graph, 0L );
			} );
			fail();
		}
		catch (EntityFilterException efe) {
			//required
		}
		try {
			scope.inTransaction(session -> {
				session.enableFilter( "filter" ).validate();
				Y y = session.find(Y.class, 0L);
			});
			fail();
		}
		catch (EntityFilterException efe) {
			//required
		}
	}

	@Entity
	@Table(name = "XX")
	@FilterDef(name = "filter",
			applyToLoadByKey = true)
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
}
