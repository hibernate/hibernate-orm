/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = CriteriaIdVersionTest.Thing.class)
class CriteriaIdVersionTest {
	@Test
	void test(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( session -> {
			session.persist( new Thing() );
		} );
		scope.inSession( session -> {
			var cb = session.getCriteriaBuilder();
			var cq = cb.createQuery( Long.class );
			var root = cq.from( Thing.class );
			cq.select( cb.id(root).as( Long.class ) );
			assertEquals( 2L, session.createSelectionQuery( cq ).getSingleResult() );
		} );
		scope.inSession( session -> {
			var cb = session.getCriteriaBuilder();
			var cq = cb.createQuery( Long.class );
			var root = cq.from( Thing.class );
			cq.select( cb.version(root).as( Long.class ) );
			assertEquals( 3L, session.createSelectionQuery( cq ).getSingleResult() );
		} );
		scope.inSession( session -> {
			var cb = session.getCriteriaBuilder();
			var cq = cb.createQuery( Long.class );
			var root = cq.from( Thing.class );
			cq.select( root.id().asLong() );
			assertEquals( 2L, session.createSelectionQuery( cq ).getSingleResult() );
		} );
	}

	@Test
	void testPath(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.inTransaction( session -> {
			Thing thing = new Thing();
			Thing otherThing = new Thing();
			otherThing.id = 5;
			thing.other = otherThing;
			session.persist( thing );
		} );
		scope.inSession( session -> {
			var cb = session.getCriteriaBuilder();
			var cq = cb.createQuery( Long.class );
			var root = cq.from( Thing.class );
			cq.select( cb.id( root.get("other") ).as( Long.class ) );
			cq.where( root.get("other").isNotNull() );
			assertEquals( 5L, session.createSelectionQuery( cq ).getSingleResult() );
		} );
	}

	@Entity
	static class Thing {
		@Id
		long id = 2;
		@Version
		long version = 3;
		@ManyToOne(cascade = CascadeType.PERSIST)
		Thing other;
	}
}
