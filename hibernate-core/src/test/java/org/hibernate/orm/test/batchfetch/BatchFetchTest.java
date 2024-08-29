/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.batchfetch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/batchfetch/ProductLine.hbm.xml",
		annotatedClasses = BatchLoadableEntity.class
)
@SessionFactory(
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false")
		}
)
public class BatchFetchTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testBatchFetch(SessionFactoryScope scope) {
		ProductLine ossProductLine = new ProductLine();
		Model hibernateModel = new Model( ossProductLine );
		scope.inTransaction(
				session -> {
					ProductLine cars = new ProductLine();
					cars.setDescription( "Cars" );
					Model monaro = new Model( cars );
					monaro.setName( "monaro" );
					monaro.setDescription( "Holden Monaro" );
					Model hsv = new Model( cars );
					hsv.setName( "hsv" );
					hsv.setDescription( "Holden Commodore HSV" );
					session.persist( cars );

					ossProductLine.setDescription( "OSS" );
					Model jboss = new Model( ossProductLine );
					jboss.setName( "JBoss" );
					jboss.setDescription( "JBoss Application Server" );

					hibernateModel.setName( "Hibernate" );
					hibernateModel.setDescription( "Hibernate" );
					Model cache = new Model( ossProductLine );
					cache.setName( "JBossCache" );
					cache.setDescription( "JBoss TreeCache" );
					session.persist( ossProductLine );
				}
		);

		scope.getSessionFactory().getCache().evictEntityData( Model.class );
		scope.getSessionFactory().getCache().evictEntityData( ProductLine.class );

		scope.inTransaction(
				session -> {
					List<ProductLine> list = session.createQuery( "from ProductLine pl order by pl.description" )
							.list();
					ProductLine cars = list.get( 0 );
					ProductLine oss = list.get( 1 );
					assertFalse( Hibernate.isInitialized( cars.getModels() ) );
					assertFalse( Hibernate.isInitialized( oss.getModels() ) );
					assertEquals( 2, cars.getModels().size() ); //fetch both collections
					assertTrue( Hibernate.isInitialized( cars.getModels() ) );
					assertTrue( Hibernate.isInitialized( oss.getModels() ) );

					session.clear();

					List<Model> models = session.createQuery( "from Model m" ).list();
					Model hibernate = session.get( Model.class, hibernateModel.getId() );
					hibernate.getProductLine().getId();
					for ( Model aList : models ) {
						assertFalse( Hibernate.isInitialized( aList.getProductLine() ) );
					}
					assertEquals( hibernate.getProductLine().getDescription(), "OSS" ); //fetch both productlines

					session.clear();

					Iterator<Model> iter = session.createQuery( "from Model" ).list().iterator();
					models = new ArrayList();
					while ( iter.hasNext() ) {
						models.add( iter.next() );
					}
					Model m = models.get( 0 );
					m.getDescription(); //fetch a batch of 4

					session.clear();

					list = session.createQuery( "from ProductLine" ).list();
					ProductLine pl = list.get( 0 );
					ProductLine pl2 = list.get( 1 );
					session.evict( pl2 );
					pl.getModels().size(); //fetch just one collection! (how can we write an assertion for that??)
				}
		);

		scope.inTransaction(
				session -> {
					List<ProductLine> list = session.createQuery( "from ProductLine pl order by pl.description" )
							.list();
					ProductLine cars = list.get( 0 );
					ProductLine oss = list.get( 1 );
					assertEquals( cars.getModels().size(), 2 );
					assertEquals( oss.getModels().size(), 3 );
					session.remove( cars );
					session.remove( oss );
				}
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testBatchFetch2(SessionFactoryScope scope) {
		int size = 32 + 14;
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < size; i++ ) {
						session.persist( new BatchLoadableEntity( i ) );
					}
				}
		);

		scope.inTransaction(
				session -> {
					// load them all as proxies
					for ( int i = 0; i < size; i++ ) {
						BatchLoadableEntity entity = session.getReference( BatchLoadableEntity.class, i );
						assertFalse( Hibernate.isInitialized( entity ) );
					}
					scope.getSessionFactory().getStatistics().clear();
					// now start initializing them...
					for ( int i = 0; i < size; i++ ) {
						BatchLoadableEntity entity = session.getReference( BatchLoadableEntity.class, i );
						Hibernate.initialize( entity );
						assertTrue( Hibernate.isInitialized( entity ) );
					}
					// so at this point, all entities are initialized.  see how many fetches were performed.
					final int expectedFetchCount;
//		if ( sessionFactory().getSettings().getBatchFetchStyle() == BatchFetchStyle.LEGACY ) {
//			expectedFetchCount = 3; // (32 + 10 + 4)
//		}
//		else if ( sessionFactory().getSettings().getBatchFetchStyle() == BatchFetchStyle.DYNAMIC ) {
//			expectedFetchCount = 2;  // (32 + 14) : because we limited batch-size to 32
//		}
//		else {
					// PADDED
					expectedFetchCount = 2; // (32 + 16*) with the 16 being padded
//		}
					assertEquals(
							expectedFetchCount,
							scope.getSessionFactory().getStatistics()
									.getEntityStatistics( BatchLoadableEntity.class.getName() )
									.getFetchCount()
					);
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete BatchLoadableEntity" ).executeUpdate();
					session.createQuery( "delete Model" ).executeUpdate();
					session.createQuery( "delete ProductLine" ).executeUpdate();
				}
		);
	}
}

