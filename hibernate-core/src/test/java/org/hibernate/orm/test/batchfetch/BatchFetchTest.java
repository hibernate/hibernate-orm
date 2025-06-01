/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.CacheSettings.USE_SECOND_LEVEL_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/batchfetch/ProductLine.xml",
		annotatedClasses = BatchLoadableEntity.class
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(settings = @Setting(name = USE_SECOND_LEVEL_CACHE, value = "false"))
public class BatchFetchTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testBatchFetch(SessionFactoryScope scope) {
		ProductLine ossProductLine = new ProductLine( "OSS" );
		Model hibernateModel = new Model( "Hibernate", "Hibernate", ossProductLine );
		scope.inTransaction( (session) -> {
			ProductLine cars = new ProductLine( "Cars" );
			new Model( "monaro", "Holden Monaro", cars );
			new Model( "hsv", "Holden Commodore HSV", cars );
			session.persist( cars );

			ossProductLine.setDescription( "OSS" );
			new Model( "JBoss", "JBoss Application Server", ossProductLine );
			new Model( "JBossCache", "JBoss TreeCache", ossProductLine );
			session.persist( ossProductLine );
		} );

		scope.inTransaction( (session) -> {
			List<ProductLine> list = session.createQuery( "from ProductLine pl order by pl.description" ).list();
			ProductLine cars = list.get( 0 );
			ProductLine oss = list.get( 1 );
			assertFalse( Hibernate.isInitialized( cars.getModels() ) );
			assertFalse( Hibernate.isInitialized( oss.getModels() ) );
			assertEquals( 2, cars.getModels().size() ); //fetch both collections
			assertTrue( Hibernate.isInitialized( cars.getModels() ) );
			assertTrue( Hibernate.isInitialized( oss.getModels() ) );
		} );

		scope.inTransaction( (session) -> {
			List<Model> models = session.createQuery( "from Model m" ).list();
			Model hibernate = session.find( Model.class, hibernateModel.getId() );
			hibernate.getProductLine().getId();
			for ( Model aList : models ) {
				assertFalse( Hibernate.isInitialized( aList.getProductLine() ) );
			}
			//fetch both product lines
			assertThat( hibernate.getProductLine().getDescription() ).isEqualTo( "OSS" );
		} );

		scope.inTransaction( (session) -> {
			Iterator<Model> iter = session.createQuery( "from Model" ).list().iterator();
			ArrayList<Model> models = new ArrayList<>();
			while ( iter.hasNext() ) {
				models.add( iter.next() );
			}
			Model m = models.get( 0 );
			m.getDescription(); //fetch a batch of 4

			session.clear();

			List<ProductLine> list = session.createQuery( "from ProductLine" ).list();
			ProductLine pl = list.get( 0 );
			ProductLine pl2 = list.get( 1 );
			session.evict( pl2 );
			pl.getModels().size(); //fetch just one collection! (how can we write an assertion for that??)
		} );

		scope.inTransaction( (session) -> {
			List<ProductLine> list = session.createQuery( "from ProductLine pl order by pl.description" ).list();
			ProductLine cars = list.get( 0 );
			ProductLine oss = list.get( 1 );
			assertThat( cars.getModels().size() ).isEqualTo( 2 );
			assertThat( oss.getModels().size() ).isEqualTo( 3 );
		} );
	}

	@Test
	public void testBatchFetch2(SessionFactoryScope scope) {
		int size = 32 + 14;
		scope.inTransaction( (session) -> {
			for ( int i = 0; i < size; i++ ) {
				session.persist( new BatchLoadableEntity( i ) );
			}
		} );

		scope.inTransaction( (session) -> {
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
			expectedFetchCount = 2; // (32 + 16*) with the 16 being padded

			assertEquals(
					expectedFetchCount,
					scope.getSessionFactory().getStatistics()
							.getEntityStatistics( BatchLoadableEntity.class.getName() )
							.getFetchCount()
			);
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}
}
