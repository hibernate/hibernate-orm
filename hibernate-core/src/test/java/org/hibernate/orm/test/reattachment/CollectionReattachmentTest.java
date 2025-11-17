/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.reattachment;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Test of collection reattachment semantics
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/reattachment/Mappings.hbm.xml"
)
@SessionFactory
public class CollectionReattachmentTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdateOwnerAfterClear(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( "p" );
					p.getChildren().add( new Child( "c" ) );
					session.persist( p );
				}
		);

		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = session.get( Parent.class, "p" );
					// clear...
					session.clear();
					// now try to reattach...
					return session.merge( p );
				}
		);

		scope.inTransaction(
				session ->
						session.remove( parent )
		);
	}

	@Test
	public void testUpdateOwnerAfterEvict(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent( "p" );
					p.getChildren().add( new Child( "c" ) );
					session.persist( p );
				}
		);

		Parent parent = scope.fromTransaction(
				session -> {
					Parent p = session.get( Parent.class, "p" );
					// evict...
					session.evict( p );
					// now try to reattach...
					return session.merge( p );
				}
		);

		scope.inTransaction(
				session ->
						session.remove( parent )
		);
	}
}
