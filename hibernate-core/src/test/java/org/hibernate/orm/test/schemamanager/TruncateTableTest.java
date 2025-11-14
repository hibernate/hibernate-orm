/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemamanager;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.GenerationType.SEQUENCE;
import static org.junit.Assert.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = TruncateTableTest.Trunk.class)
class TruncateTableTest {
	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Trunk trunk = new Trunk();
			session.persist( trunk );
			session.persist( new Trunk(trunk) );
			session.persist( new Trunk(trunk) );
			assertEquals( 1, trunk.id );
		} );
		scope.getSessionFactory().getSchemaManager()
				.truncateTable("trunk");
		scope.inTransaction( session -> {
			assertEquals(0,
					session.createQuery( "from Trunk", Trunk.class )
							.getResultCount());
		} );
		scope.inTransaction( session -> {
			Trunk trunk = new Trunk();
			session.persist( trunk );
			assertEquals( 1, trunk.id );
		} );
	}
	@Entity(name = "Trunk")
	@Table(name = "trunk")
	static class Trunk {
		@GeneratedValue(strategy = SEQUENCE) @Id
		long id;
		@ManyToOne Trunk parent;

		Trunk() {}
		Trunk(Trunk parent) {
			this.parent = parent;
		}
	}
}
