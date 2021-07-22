/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.legacy;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Assertions that reflection does not happen during the building of
 * Hibernate's boot (o.h.mapping) model.
 *
 * None of the classes referenced in the XML mapping actually exist
 */
@ServiceRegistry(
		settings = @Setting( name = "javax.persistence.validation.mode", value = "none" )
)
@BaseUnitTest
public class NonReflectiveBinderTest {

	private InFlightMetadataCollector createMetadataCollector(ServiceRegistryScope scope) {
		final MetadataBuilderImpl metadataBuilder = (MetadataBuilderImpl) new MetadataSources( scope.getRegistry() )
				.addResource( "org/hibernate/orm/test/legacy/Wicked.hbm.xml" )
				.getMetadataBuilder();
		return metadataBuilder.buildMetadataCollector();
	}

	@Test
	public void testMetaInheritance(ServiceRegistryScope scope) {
		final InFlightMetadataCollector metadataCollector = createMetadataCollector( scope );
		final PersistentClass wickedMapping = metadataCollector.getEntityBinding( "org.hibernate.orm.test.legacy.Wicked" );

		final Map m = wickedMapping.getMetaAttributes();
		assertNotNull( m );
		assertNotNull( wickedMapping.getMetaAttribute( "global" ) );
		assertNull( wickedMapping.getMetaAttribute( "globalnoinherit" ) );

		final MetaAttribute metaAttribute = wickedMapping.getMetaAttribute( "implements" );
		assertNotNull( metaAttribute );
		assertThat( metaAttribute.getName(), is( "implements" ) );
		assertTrue( metaAttribute.isMultiValued() );
		assertThat( metaAttribute.getValues().size(), is( 3 ) );
		assertThat( metaAttribute.getValues().get( 0 ), is( "java.lang.Observer" ) );
		assertThat( metaAttribute.getValues().get( 1 ), is( "java.lang.Observer" ) );
		assertThat( metaAttribute.getValues().get( 2 ), is( "org.foo.BogusVisitor" ) );
				
		/*Property property = wickedMapping.getIdentifierProperty();
		property.getMetaAttribute(null);*/

		final Iterator<Property> propertyIterator = wickedMapping.getPropertyIterator();
		while ( propertyIterator.hasNext() ) {
			Property element = propertyIterator.next();
			Map ma = element.getMetaAttributes();
			assertNotNull( ma );
			assertNotNull( element.getMetaAttribute( "global" ) );
			MetaAttribute metaAttribute2 = element.getMetaAttribute( "implements" );
			assertNotNull( metaAttribute2 );
			assertNull( element.getMetaAttribute( "globalnoinherit" ) );

		}

		final Property element = wickedMapping.getProperty( "component" );
		final Map ma = element.getMetaAttributes();
		assertNotNull( ma );
		assertNotNull( element.getMetaAttribute( "global" ) );
		assertNotNull( element.getMetaAttribute( "componentonly" ) );
		assertNotNull( element.getMetaAttribute( "allcomponent" ) );
		assertNull( element.getMetaAttribute( "globalnoinherit" ) );

		final MetaAttribute compimplements = element.getMetaAttribute( "implements" );
		assertNotNull( compimplements );
		assertThat( compimplements.getValue(), is( "AnotherInterface" ) );

		final Property xp = ( (Component) element.getValue() ).getProperty( "x" );
		final MetaAttribute propximplements = xp.getMetaAttribute( "implements" );
		assertNotNull( propximplements );
		assertThat( propximplements.getValue(), is( "AnotherInterface" ) );


	}

	@Test
	@TestForIssue(jiraKey = "HBX-718")
	public void testNonMutatedInheritance(ServiceRegistryScope scope) {
		final InFlightMetadataCollector metadataCollector = createMetadataCollector( scope );
		final PersistentClass wickedMapping = metadataCollector.getEntityBinding( "org.hibernate.orm.test.legacy.Wicked" );

		MetaAttribute metaAttribute = wickedMapping.getMetaAttribute( "globalmutated" );

		assertNotNull( metaAttribute );
		/*assertEquals( metaAttribute.getValues().size(), 2 );		
		assertEquals( "top level", metaAttribute.getValues().get(0) );*/
		assertThat( metaAttribute.getValue(), is( "wicked level" ) );

		Property property = wickedMapping.getProperty( "component" );
		MetaAttribute propertyAttribute = property.getMetaAttribute( "globalmutated" );

		assertNotNull( propertyAttribute );
		/*assertEquals( propertyAttribute.getValues().size(), 3 );
		assertEquals( "top level", propertyAttribute.getValues().get(0) );
		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );*/
		assertThat( propertyAttribute.getValue(), is( "monetaryamount level" ) );

		org.hibernate.mapping.Component component = (Component) property.getValue();
		property = component.getProperty( "x" );
		propertyAttribute = property.getMetaAttribute( "globalmutated" );

		assertNotNull( propertyAttribute );
		/*assertEquals( propertyAttribute.getValues().size(), 4 );
		assertEquals( "top level", propertyAttribute.getValues().get(0) );
		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );
		assertEquals( "monetaryamount level", propertyAttribute.getValues().get(2) );*/
		assertThat( propertyAttribute.getValue(), is( "monetaryamount x level" ) );

		property = wickedMapping.getProperty( "sortedEmployee" );
		propertyAttribute = property.getMetaAttribute( "globalmutated" );

		assertNotNull( propertyAttribute );
		/*assertEquals( propertyAttribute.getValues().size(), 3 );
		assertEquals( "top level", propertyAttribute.getValues().get(0) );
		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );*/
		assertThat( propertyAttribute.getValue(), is( "sortedemployee level" ) );

		property = wickedMapping.getProperty( "anotherSet" );
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
	public void testComparator(ServiceRegistryScope scope) {
		final InFlightMetadataCollector metadataCollector = createMetadataCollector( scope );
		final PersistentClass wickedMapping = metadataCollector.getEntityBinding( "org.hibernate.orm.test.legacy.Wicked" );
		final Property property = wickedMapping.getProperty( "sortedEmployee" );
		final Collection col = (Collection) property.getValue();

		assertThat( col.getComparatorClassName(), is( "org.hibernate.orm.test.legacy.NonExistingComparator" ) );
	}
}
