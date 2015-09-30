/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;

/**
 * Test simple application of Convert annotation via XML.
 *
 * @author Steve Ebersole
 */
public class SimpleXmlOverriddenTest extends BaseUnitTestCase {
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

	/**
	 * A baseline test, with an explicit @Convert annotation that should be in effect
	 */
	@Test
	public void baseline() {
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( TheEntity.class )
				.buildMetadata();

		PersistentClass pc = metadata.getEntityBinding( TheEntity.class.getName() );
		Type type = pc.getProperty( "it" ).getType();
		AttributeConverterTypeAdapter adapter = assertTyping( AttributeConverterTypeAdapter.class, type );
		assertTyping( SillyStringConverter.class, adapter.getAttributeConverter() );
	}

	/**
	 * Test outcome of applying overrides via orm.xml, specifically at the attribute level
	 */
	@Test
	public void testDefinitionAtAttributeLevel() {
		// NOTE : simple-override.xml applied disable-conversion="true" at the attribute-level
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( TheEntity.class )
				.addResource( "org/hibernate/test/converter/simple-override.xml" )
				.buildMetadata();

		PersistentClass pc = metadata.getEntityBinding( TheEntity.class.getName() );
		Type type = pc.getProperty( "it" ).getType();
		assertTyping( StringType.class, type );
	}

	/**
	 * Test outcome of applying overrides via orm.xml, specifically at the entity level
	 */
	@Test
	public void testDefinitionAtEntityLevel() {
		// NOTE : simple-override2.xml applied disable-conversion="true" at the entity-level
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( TheEntity2.class )
				.addResource( "org/hibernate/test/converter/simple-override2.xml" )
				.buildMetadata();

		PersistentClass pc = metadata.getEntityBinding( TheEntity2.class.getName() );
		Type type = pc.getProperty( "it" ).getType();
		assertTyping( StringType.class, type );
	}

	@Entity(name="TheEntity")
	public static class TheEntity {
		@Id
		public Integer id;
		@Convert(converter = SillyStringConverter.class)
		public String it;
	}

	@Entity(name="TheEntity2")
	@Convert( attributeName = "it", converter = SillyStringConverter.class )
	public static class TheEntity2 {
		@Id
		public Integer id;
		public String it;
	}
}
