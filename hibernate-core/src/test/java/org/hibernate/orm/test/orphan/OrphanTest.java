/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.orphan;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/orphan/Product.hbm.xml"
)
@SessionFactory
public class OrphanTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteOnDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product prod = new Product();
					prod.setName( "Widget" );
					Part part = new Part();
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
					session.flush();

					prod.getParts().remove( part );

					session.remove( prod );
				}
		);


		scope.inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNull( session.get( Part.class, "Get" ) );
					assertNull( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteAfterPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product prod = new Product();
					prod.setName( "Widget" );
					Part part = new Part();
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );

					prod.getParts().remove( part );
				}
		);

		scope.inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.remove( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteAfterPersistAndFlush(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product prod = new Product();
					prod.setName( "Widget" );
					Part part = new Part();
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
					session.flush();

					prod.getParts().remove( part );
				}
		);

		scope.inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.remove( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCannotLockDetachedEntity(SessionFactoryScope scope) {
		Product prod = new Product();
		Part part = new Part();
		scope.inTransaction(
				session -> {
					prod.setName( "Widget" );
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
				}
		);


		scope.inTransaction(
				session -> {
					assertThrows(IllegalArgumentException.class,
								() -> session.lock( prod, LockMode.READ ),
								"Given entity is not associated with the persistence context"
					);
				}
		);

		scope.inTransaction(
				session -> {
					assertNotNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.remove( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product prod = new Product();
					prod.setName( "Widget" );
					Part part = new Part();
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
				}
		);

		scope.getSessionFactory().getCache().evictEntityData( Product.class );
		scope.getSessionFactory().getCache().evictEntityData( Part.class );


		scope.inTransaction(
				session -> {
					Product prod = session.get( Product.class, "Widget" );
					assertTrue( Hibernate.isInitialized( prod.getParts() ) );
					Part part = session.get( Part.class, "Widge" );
					prod.getParts().remove( part );
				}
		);

		scope.inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.remove( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteOnMerge(SessionFactoryScope scope) {
		Product prod = new Product();
		Part part = new Part();

		scope.inTransaction(
				session -> {
					prod.setName( "Widget" );
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					Part part2 = new Part();
					part2.setName( "Get" );
					part2.setDescription( "another part if a Widget" );
					prod.getParts().add( part2 );
					session.persist( prod );
				}
		);

		prod.getParts().remove( part );

		scope.inTransaction(
				session ->
						session.merge( prod )
		);

		scope.inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.remove( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOrphanDeleteOnMergeRemoveElementMerge(SessionFactoryScope scope) {
		Product prod = new Product();
		Part part = new Part();
		scope.inTransaction(
				session -> {
					prod.setName( "Widget" );
					part.setName( "Widge" );
					part.setDescription( "part if a Widget" );
					prod.getParts().add( part );
					session.persist( prod );
				}
		);

		scope.inTransaction(
				session -> {
					session.merge( prod );
					prod.getParts().remove( part );
					session.merge( prod );
				}
		);

		scope.inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					session.remove( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	@JiraKey(value = "HHH-9171")
	public void testOrphanDeleteOnAddElementMergeRemoveElementMerge(SessionFactoryScope scope) {
		Product prod = new Product();
		scope.inTransaction(
				session -> {
					prod.setName( "Widget" );
					session.persist( prod );
				}
		);

		Part part = new Part();
		part.setName( "Widge" );
		part.setDescription( "part if a Widget" );
		prod.getParts().add( part );

		scope.inTransaction(
				session -> {
					session.merge( prod );
					// In Section 2.9, Entity Relationships, the JPA 2.1 spec says:
					// "If the entity being orphaned is a detached, new, or removed entity,
					// the semantics of orphanRemoval do not apply."
					// In other words, since part is a new entity, it will not be deleted when removed
					// from prod.parts, even though cascade for the association includes "delete-orphan".
					prod.getParts().remove( part );
					session.merge( prod );
				}
		);

		scope.inTransaction(
				session -> {
					assertNotNull( session.get( Part.class, "Widge" ) );
					session.remove( session.get( Product.class, "Widget" ) );
				}
		);
	}
}
