/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.backref;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;
import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;
import org.hibernate.orm.test.collection.backref.map.compkey.MapKey;
import org.hibernate.orm.test.collection.backref.map.compkey.Part;
import org.hibernate.orm.test.collection.backref.map.compkey.Product;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * BackrefCompositeMapKeyTest implementation.  Test access to a composite map-key
 * backref via a number of different access methods.
 *
 * @author Steve Ebersole
 */
@RunWith( BytecodeEnhancerRunner.class )
@CustomEnhancementContext({ NoDirtyCheckingContext.class, DirtyCheckEnhancementContext.class })
public class BackrefCompositeMapKeyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] {
				"org/hibernate/orm/test/collection/backref/map/compkey/Mappings.hbm.xml"
		};
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
					assertNull( "Orphan 'Widge' was not deleted", session.get( Part.class, "Widge" ) );
					assertNull( "Orphan 'Get' was not deleted", session.get( Part.class, "Get" ) );
					assertNull( "Orphan 'Widget' was not deleted", session.get( Product.class, "Widget" ) );
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
				session ->
						session.delete( session.get( Product.class, "Widget" ) )
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
				session ->
						session.saveOrUpdate( prod )
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

		Product cloned = (Product) SerializationHelper.clone( prod );

		inTransaction(
				session ->
						session.saveOrUpdate( cloned )
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


		SessionFactoryImplementor sessionFactory = sessionFactory();
		sessionFactory.getCache().evictEntityData( Product.class );
		sessionFactory.getCache().evictEntityData( Part.class );

		inTransaction(
				session -> {
					Product prod = session.get( Product.class, "Widget" );
					assertTrue( Hibernate.isInitialized( prod.getParts() ) );
					Part part = session.get( Part.class, "Widge" );
					prod.getParts().remove( mapKey );
				}
		);


		sessionFactory.getCache().evictEntityData( Product.class );
		sessionFactory.getCache().evictEntityData( Part.class );

		inTransaction(
				session -> {
					Product prod = session.get( Product.class, "Widget" );
					assertTrue( Hibernate.isInitialized( prod.getParts() ) );
					assertNull( prod.getParts().get( new MapKey( "Top" ) ) );
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
				session ->
						session.merge( prod )
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
