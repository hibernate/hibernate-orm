/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.backref.map.compkey;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * BackrefCompositeMapKeyTest implementation.  Test access to a composite map-key
 * backref via a number of different access methods.
 *
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = (
				"org/hibernate/orm/test/collection/backref/map/compkey/Mappings.hbm.xml"
		)
)
@SessionFactory
public class BackrefCompositeMapKeyTest {

	@Test
	public void testOrphanDeleteOnDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product prod = new Product( "Widget" );
					Part part = new Part( "Widge", "part if a Widget" );
					MapKey mapKey = new MapKey( "Top" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );
					session.flush();

					prod.getParts().remove( mapKey );

					session.remove( prod );
				}
		);

		scope.inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ), "Orphan 'Widge' was not deleted" );
					assertNull( session.get( Part.class, "Get" ), "Orphan 'Get' was not deleted" );
					assertNull( session.get( Product.class, "Widget" ), "Orphan 'Widget' was not deleted" );
				}
		);
	}

	@Test
	public void testOrphanDeleteAfterPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product prod = new Product( "Widget" );
					Part part = new Part( "Widge", "part if a Widget" );
					MapKey mapKey = new MapKey( "Top" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );

					prod.getParts().remove( mapKey );
				}
		);

		scope.inTransaction(
				session ->
						session.remove( session.get( Product.class, "Widget" ) )
		);
	}

	@Test
	public void testOrphanDeleteAfterPersistAndFlush(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product prod = new Product( "Widget" );
					Part part = new Part( "Widge", "part if a Widget" );
					MapKey mapKey = new MapKey( "Top" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );
					session.flush();

					prod.getParts().remove( mapKey );
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
	public void testCannotLockDetachedEntity(SessionFactoryScope scope) {
		Product prod = new Product( "Widget" );
		MapKey mapKey = new MapKey( "Top" );
		scope.inTransaction(
				session -> {
					Part part = new Part( "Widge", "part if a Widget" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
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
	public void testOrphanDelete(SessionFactoryScope scope) {
		MapKey mapKey = new MapKey( "Top" );
		scope.inTransaction(
				session -> {
					Product prod = new Product( "Widget" );
					Part part = new Part( "Widge", "part if a Widget" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );
				}
		);


		SessionFactoryImplementor sessionFactory = scope.getSessionFactory();
		sessionFactory.getCache().evictEntityData( Product.class );
		sessionFactory.getCache().evictEntityData( Part.class );

		scope.inTransaction(
				session -> {
					Product prod = session.get( Product.class, "Widget" );
					assertTrue( Hibernate.isInitialized( prod.getParts() ) );
					Part part = session.get( Part.class, "Widge" );
					prod.getParts().remove( mapKey );
				}
		);


		sessionFactory.getCache().evictEntityData( Product.class );
		sessionFactory.getCache().evictEntityData( Part.class );

		scope.inTransaction(
				session -> {
					Product prod = session.get( Product.class, "Widget" );
					assertTrue( Hibernate.isInitialized( prod.getParts() ) );
					assertNull( prod.getParts().get( new MapKey( "Top" ) ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.remove( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	public void testOrphanDeleteOnMerge(SessionFactoryScope scope) {
		Product prod = new Product( "Widget" );
		MapKey mapKey = new MapKey( "Top" );
		scope.inTransaction(
				session -> {
					Part part = new Part( "Widge", "part if a Widget" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );
				}
		);


		prod.getParts().remove( mapKey );

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
}
