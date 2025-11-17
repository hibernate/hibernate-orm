/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@BaseUnitTest
public class NonReflectiveBinderTest {
	private StandardServiceRegistry ssr;
	private Metadata metadata;

	@BeforeEach
	public void setUp() throws Exception {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "jakarta.persistence.validation.mode", "none" )
				.build();
		metadata = new MetadataSources( ssr )
				.addResource( "org/hibernate/orm/test/legacy/Wicked.hbm.xml" )
				.buildMetadata();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testMetaInheritance() {
		PersistentClass cm = metadata.getEntityBinding( "org.hibernate.orm.test.legacy.Wicked" );
		Map m = cm.getMetaAttributes();
		assertNotNull( m );
		assertNotNull( cm.getMetaAttribute( "global" ) );
		assertNull( cm.getMetaAttribute( "globalnoinherit" ) );

		MetaAttribute metaAttribute = cm.getMetaAttribute( "implements" );
		assertNotNull( metaAttribute );
		assertThat( metaAttribute.getName(), is( "implements" ) );
		assertTrue( metaAttribute.isMultiValued() );
		assertThat( metaAttribute.getValues().size(), is( 3 ) );
		assertThat( metaAttribute.getValues().get( 0 ), is( "java.lang.Observer" ) );
		assertThat( metaAttribute.getValues().get( 1 ), is( "java.lang.Observer" ) );
		assertThat( metaAttribute.getValues().get( 2 ), is( "org.foo.BogusVisitor" ) );

		/*Property property = cm.getIdentifierProperty();
		property.getMetaAttribute(null);*/

		Iterator<Property> propertyIterator = cm.getProperties().iterator();
		while ( propertyIterator.hasNext() ) {
			Property element = propertyIterator.next();
			Map ma = element.getMetaAttributes();
			assertNotNull( ma );
			assertNotNull( element.getMetaAttribute( "global" ) );
			MetaAttribute metaAttribute2 = element.getMetaAttribute( "implements" );
			assertNotNull( metaAttribute2 );
			assertNull( element.getMetaAttribute( "globalnoinherit" ) );

		}

		Property element = cm.getProperty( "component" );
		Map ma = element.getMetaAttributes();
		assertNotNull( ma );
		assertNotNull( element.getMetaAttribute( "global" ) );
		assertNotNull( element.getMetaAttribute( "componentonly" ) );
		assertNotNull( element.getMetaAttribute( "allcomponent" ) );
		assertNull( element.getMetaAttribute( "globalnoinherit" ) );

		MetaAttribute compimplements = element.getMetaAttribute( "implements" );
		assertNotNull( compimplements );
		assertThat( compimplements.getValue(), is( "AnotherInterface" ) );

		Property xp = ( (Component) element.getValue() ).getProperty( "x" );
		MetaAttribute propximplements = xp.getMetaAttribute( "implements" );
		assertNotNull( propximplements );
		assertThat( propximplements.getValue(), is( "AnotherInterface" ) );


	}

	@Test
	@JiraKey(value = "HBX-718")
	public void testNonMutatedInheritance() {
		PersistentClass cm = metadata.getEntityBinding( "org.hibernate.orm.test.legacy.Wicked" );
		MetaAttribute metaAttribute = cm.getMetaAttribute( "globalmutated" );

		assertNotNull( metaAttribute );
		/*assertEquals( metaAttribute.getValues().size(), 2 );
		assertEquals( "top level", metaAttribute.getValues().get(0) );*/
		assertThat( metaAttribute.getValue(), is( "wicked level" ) );

		Property property = cm.getProperty( "component" );
		MetaAttribute propertyAttribute = property.getMetaAttribute( "globalmutated" );

		assertNotNull( propertyAttribute );
		/*assertEquals( propertyAttribute.getValues().size(), 3 );
		assertEquals( "top level", propertyAttribute.getValues().get(0) );
		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );*/
		assertThat( propertyAttribute.getValue(), is( "monetaryamount level" ) );

		Component component = (Component) property.getValue();
		property = component.getProperty( "x" );
		propertyAttribute = property.getMetaAttribute( "globalmutated" );

		assertNotNull( propertyAttribute );
		/*assertEquals( propertyAttribute.getValues().size(), 4 );
		assertEquals( "top level", propertyAttribute.getValues().get(0) );
		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );
		assertEquals( "monetaryamount level", propertyAttribute.getValues().get(2) );*/
		assertThat( propertyAttribute.getValue(), is( "monetaryamount x level" ) );

		property = cm.getProperty( "sortedEmployee" );
		propertyAttribute = property.getMetaAttribute( "globalmutated" );

		assertNotNull( propertyAttribute );
		/*assertEquals( propertyAttribute.getValues().size(), 3 );
		assertEquals( "top level", propertyAttribute.getValues().get(0) );
		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );*/
		assertThat( propertyAttribute.getValue(), is( "sortedemployee level" ) );

		property = cm.getProperty( "anotherSet" );
		propertyAttribute = property.getMetaAttribute( "globalmutated" );

		assertNotNull( propertyAttribute );
		/*assertEquals( propertyAttribute.getValues().size(), 2 );
		assertEquals( "top level", propertyAttribute.getValues().get(0) );*/
		assertThat( propertyAttribute.getValue(), is( "wicked level" ) );

		Bag bag = (Bag) property.getValue();
		component = (Component) bag.getElement();

		assertThat( component.getMetaAttributes().size(), is( 4 ) );

		metaAttribute = component.getMetaAttribute( "globalmutated" );
		/*assertEquals( metaAttribute.getValues().size(), 3 );
		assertEquals( "top level", metaAttribute.getValues().get(0) );
		assertEquals( "wicked level", metaAttribute.getValues().get(1) );*/
		assertThat( metaAttribute.getValue(), is( "monetaryamount anotherSet composite level" ) );

		property = component.getProperty( "emp" );
		propertyAttribute = property.getMetaAttribute( "globalmutated" );

		assertNotNull( propertyAttribute );
		/*assertEquals( propertyAttribute.getValues().size(), 4 );
		assertEquals( "top level", propertyAttribute.getValues().get(0) );
		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );
		assertEquals( "monetaryamount anotherSet composite level", propertyAttribute.getValues().get(2) );*/
		assertThat( propertyAttribute.getValue(), is( "monetaryamount anotherSet composite property emp level" ) );


		property = component.getProperty( "empinone" );
		propertyAttribute = property.getMetaAttribute( "globalmutated" );

		assertNotNull( propertyAttribute );
		/*assertEquals( propertyAttribute.getValues().size(), 4 );
		assertEquals( "top level", propertyAttribute.getValues().get(0) );
		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );
		assertEquals( "monetaryamount anotherSet composite level", propertyAttribute.getValues().get(2) );*/
		assertThat( propertyAttribute.getValue(), is( "monetaryamount anotherSet composite property empinone level" ) );
	}

	@Test
	public void testComparator() {
		PersistentClass cm = metadata.getEntityBinding( "org.hibernate.orm.test.legacy.Wicked" );

		Property property = cm.getProperty( "sortedEmployee" );
		Collection col = (Collection) property.getValue();
		assertThat( col.getComparatorClassName(), is( "org.hibernate.orm.test.legacy.NonExistingComparator" ) );
	}
}
