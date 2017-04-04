/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy.synchronizedTables;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class SynchronizeTableNamingTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;

	@Before
	public void before() {
		ssr = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testAnnotationHandling() {
		final Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( DynamicEntity.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( TestingPhysicalNamingStrategy.INSTANCE )
				.build();
		verify( metadata.getEntityBinding( DynamicEntity.class.getName() ) );
	}

	@Test
	public void testHbmXmlHandling() {
		final Metadata metadata = new MetadataSources( ssr )
				.addResource( "org/hibernate/test/namingstrategy/synchronizedTables/mapping.hbm.xml" )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( TestingPhysicalNamingStrategy.INSTANCE )
				.build();
		verify( metadata.getEntityBinding( DynamicEntity.class.getName() ) );
	}

	private void verify(PersistentClass entityBinding) {
		assertTrue( entityBinding.getSynchronizedTables().contains( "tbl_a" ) );
	}

	public static class TestingPhysicalNamingStrategy extends PhysicalNamingStrategyStandardImpl {
		/**
		 * Singleton access
		 */
		public static final TestingPhysicalNamingStrategy INSTANCE = new TestingPhysicalNamingStrategy();

		@Override
		public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
			String baseName = name.render( context.getDialect() );
			if ( baseName.equals( "table_a" ) ) {
				baseName = "tbl_a";
			}
			return context.getIdentifierHelper().toIdentifier( baseName );
		}
	}
}
