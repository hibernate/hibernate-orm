/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.backref.map.compkey;

import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.internal.util.SerializationHelper;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * BackrefCompositeMapKeyTest implementation.  Test access to a composite map-key
 * backref via a number of different access methods.
 *
 * @author Steve Ebersole
 */
public class BackrefCompositeMapKeyTest extends SessionFactoryBasedFunctionalTest {
	@Override
	public String[] getHmbMappingFiles() {
		return new String[] { "collection/backref/map/compkey/Mappings.hbm.xml" };
	}

	@Test
	public void testOrphanDeleteOnDelete() {
		inTransaction(
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

					session.delete( prod );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ), "Orphan 'Widge' was not deleted" );
					assertNull( session.get( Part.class, "Get" ), "Orphan 'Get' was not deleted" );
					assertNull( session.get( Product.class, "Widget" ), "Orphan 'Widget' was not deleted" );

				}
		);
	}

	@Test
	public void testOrphanDeleteAfterPersist() {
		inTransaction(
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

		inTransaction(
				session -> {
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	public void testOrphanDeleteAfterPersistAndFlush() {

		inTransaction(
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

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	@Disabled("lock has not yet been implemented")
	public void testOrphanDeleteAfterLock() {
		Product prod = new Product( "Widget" );
		MapKey mapKey = new MapKey( "Top" );
		inTransaction(
				session -> {
					Part part = new Part( "Widge", "part if a Widget" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );
				}
		);

		inTransaction(
				session -> {
					session.lock( prod, LockMode.READ );
					prod.getParts().remove( mapKey );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	public void testOrphanDeleteOnSaveOrUpdate() {
		Product prod = new Product( "Widget" );
		MapKey mapKey = new MapKey( "Top" );

		inTransaction(
				session -> {
					Part part = new Part( "Widge", "part if a Widget" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );
				}
		);

		prod.getParts().remove( mapKey );

		inTransaction(
				session -> {
					session.saveOrUpdate( prod );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	public void testOrphanDeleteOnSaveOrUpdateAfterSerialization() {
		final Product prod = new Product( "Widget" );
		MapKey mapKey = new MapKey( "Top" );
		inTransaction(
				session -> {
					Part part = new Part( "Widge", "part if a Widget" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );

				}
		);

		prod.getParts().remove( mapKey );

		Product cloned = (Product) SerializationHelper.clone( prod );

		inTransaction(
				session -> {
					session.saveOrUpdate( cloned );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	public void testOrphanDelete() {

		MapKey mapKey = new MapKey( "Top" );
		inTransaction(
				session -> {
					Product prod = new Product( "Widget" );
					Part part = new Part( "Widge", "part if a Widget" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );

				}
		);

		sessionFactory().getCache().evictEntityRegion( Product.class );
		sessionFactory().getCache().evictEntityRegion( Part.class );

		inTransaction(
				session -> {
					Product prod = session.get( Product.class, "Widget" );
					assertTrue( Hibernate.isInitialized( prod.getParts() ) );
					Map parts = prod.getParts();
					assertThat( parts.size(), is(2) );
					session.get( Part.class, "Widge" );
					parts.remove( mapKey );
				}
		);

		sessionFactory().getCache().evictEntityRegion( Product.class );
		sessionFactory().getCache().evictEntityRegion( Part.class );

		inTransaction(
				session -> {
					Product prod = session.get( Product.class, "Widget" );
					assertTrue( Hibernate.isInitialized( prod.getParts() ) );
					Map parts = prod.getParts();
					assertThat( parts.size(), is( 1 ) );
					assertNull( parts.get( new MapKey( "Top" ) ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}

	@Test
	public void testOrphanDeleteOnMerge() {
		Product prod = new Product( "Widget" );
		MapKey mapKey = new MapKey( "Top" );
		inTransaction(
				session -> {
					Part part = new Part( "Widge", "part if a Widget" );
					prod.getParts().put( mapKey, part );
					Part part2 = new Part( "Get", "another part if a Widget" );
					prod.getParts().put( new MapKey( "Bottom" ), part2 );
					session.persist( prod );
				}
		);

		prod.getParts().remove( mapKey );

		inTransaction(
				session -> {
					session.merge( prod );
				}
		);

		inTransaction(
				session -> {
					assertNull( session.get( Part.class, "Widge" ) );
					assertNotNull( session.get( Part.class, "Get" ) );
					session.delete( session.get( Product.class, "Widget" ) );
				}
		);
	}
}
