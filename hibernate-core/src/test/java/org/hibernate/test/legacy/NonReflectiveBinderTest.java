/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.legacy;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.MetaAttribute;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@FailureExpectedWithNewMetamodel
public class NonReflectiveBinderTest extends BaseUnitTestCase {
	private MetadataImplementor metadata;

	// metamodel : very strange error where the XML references a class that clearly does not exist.  How did this ever work?

	@Before
	public void setUp() throws Exception {
		MetadataSources metadataSources = new MetadataSources()
				.addResource( "org/hibernate/test/legacy/Wicked.hbm.xml" );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
	}

	@Test
	public void testMetaInheritance() {
		EntityBinding eb = metadata.getEntityBinding( "org.hibernate.test.legacy.Wicked" );
		assertNotNull( eb );
		assertNotNull( eb.getMetaAttributeContext() );
		assertNotNull( eb.getMetaAttributeContext().getMetaAttribute( "global" ) );
		assertNull( eb.getMetaAttributeContext().getMetaAttribute( "globalnoinherit" ) );

		MetaAttribute metaAttribute = eb.getMetaAttributeContext().getMetaAttribute( "implements" );
		assertNotNull( metaAttribute );
		assertEquals( "implements", metaAttribute.getName() );
		assertTrue( metaAttribute.isMultiValued() );
		assertEquals( 3, metaAttribute.getValues().size() );
		assertEquals( "java.lang.Observer",metaAttribute.getValues().get(0) );
		assertEquals( "java.lang.Observer",metaAttribute.getValues().get(1) );
		assertEquals( "org.foo.BogusVisitor",metaAttribute.getValues().get(2) );

		for ( AttributeBinding attributeBinding : eb.getAttributeBindingClosure() ) {
			assertNotNull( attributeBinding.getMetaAttributeContext() );
			assertNotNull( attributeBinding.getMetaAttributeContext().getMetaAttribute( "global" ) );
			MetaAttribute metaAttribute2 = attributeBinding.getMetaAttributeContext().getMetaAttribute( "implements" );
			assertNotNull( metaAttribute2 );
			assertNull( attributeBinding.getMetaAttributeContext().getMetaAttribute( "globalnoinherit" ) );
		}

		EmbeddedAttributeBinding componentAttributeBinding = (EmbeddedAttributeBinding) eb.locateAttributeBinding( "component" );
		assertNotNull( componentAttributeBinding.getMetaAttributeContext() );
		assertNotNull( componentAttributeBinding.getMetaAttributeContext().getMetaAttribute( "global" ) );
		assertNotNull( componentAttributeBinding.getMetaAttributeContext().getMetaAttribute( "componentonly" ) );
		assertNotNull( componentAttributeBinding.getMetaAttributeContext().getMetaAttribute( "allcomponent" ) );
		assertNull( componentAttributeBinding.getMetaAttributeContext().getMetaAttribute( "globalnoinherit" ) );

		MetaAttribute componentImplementsMeta = componentAttributeBinding.getMetaAttributeContext().getMetaAttribute( "implements" );
		assertNotNull( componentImplementsMeta );
		assertEquals( componentImplementsMeta.getValue(), "AnotherInterface" );

		AttributeBinding xAttributeBinding = componentAttributeBinding.getEmbeddableBinding().locateAttributeBinding( "x" );
		MetaAttribute xImplementsMeta = xAttributeBinding.getMetaAttributeContext().getMetaAttribute( "implements" );
		assertNotNull( xImplementsMeta );
		assertEquals( xImplementsMeta.getValue(), "AnotherInterface" );
	}

	@Test
	@TestForIssue( jiraKey = "HBX-718" )
	public void testNonMutatedInheritance() {
		EntityBinding eb = metadata.getEntityBinding( "org.hibernate.test.legacy.Wicked" );
		assertNotNull( eb );

		MetaAttribute metaAttribute = eb.getMetaAttributeContext().getMetaAttribute( "globalmutated" );
		assertNotNull(metaAttribute);
		assertEquals( "wicked level", metaAttribute.getValue() );

		EmbeddedAttributeBinding componentAttributeBinding = (EmbeddedAttributeBinding) eb.locateAttributeBinding( "component" );
		MetaAttribute propertyAttribute = componentAttributeBinding.getMetaAttributeContext().getMetaAttribute( "globalmutated" );
		assertNotNull( propertyAttribute );
		assertEquals( "monetaryamount level", propertyAttribute.getValue() );

		AttributeBinding xAttributeBinding = componentAttributeBinding.getEmbeddableBinding().locateAttributeBinding( "x" );
		propertyAttribute = xAttributeBinding.getMetaAttributeContext().getMetaAttribute( "globalmutated" );
		assertNotNull( propertyAttribute );
		assertEquals( "monetaryamount x level", propertyAttribute.getValue() );

		PluralAttributeBinding sortedEmployeeBinding = (PluralAttributeBinding) eb.locateAttributeBinding( "sortedEmployee" );
		propertyAttribute = sortedEmployeeBinding.getMetaAttributeContext().getMetaAttribute( "globalmutated" );
		assertNotNull(propertyAttribute);
		assertEquals( "sortedemployee level", propertyAttribute.getValue() );

		PluralAttributeBinding anotherSetBinding = (PluralAttributeBinding) eb.locateAttributeBinding( "anotherSet" );
		propertyAttribute = anotherSetBinding.getMetaAttributeContext().getMetaAttribute( "globalmutated" );
		assertNotNull( propertyAttribute );
		assertEquals( "wicked level", propertyAttribute.getValue() );

		// todo : need to make expand meta-attribute coverage
//		PluralAttributeElementBinding elementBinding = anotherSetBinding.getPluralAttributeElementBinding();
//		assertEquals( 4, elementBinding.getMetaAttributes().size() );
//
//		Bag bag = (Bag) property.getValue();
//		component = (Component)bag.getElement();
//
//		assertEquals(4,component.getMetaAttributes().size());
//
//		metaAttribute = component.getMetaAttribute( "globalmutated" );
//		/*assertEquals( metaAttribute.getValues().size(), 3 );
//		assertEquals( "top level", metaAttribute.getValues().get(0) );
//		assertEquals( "wicked level", metaAttribute.getValues().get(1) );*/
//		assertEquals( "monetaryamount anotherSet composite level", metaAttribute.getValue() );
//
//		property = component.getProperty( "emp" );
//		propertyAttribute = property.getMetaAttribute( "globalmutated" );
//
//		assertNotNull(propertyAttribute);
//		/*assertEquals( propertyAttribute.getValues().size(), 4 );
//		assertEquals( "top level", propertyAttribute.getValues().get(0) );
//		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );
//		assertEquals( "monetaryamount anotherSet composite level", propertyAttribute.getValues().get(2) );*/
//		assertEquals( "monetaryamount anotherSet composite property emp level", propertyAttribute.getValue() );
//
//
//		property = component.getProperty( "empinone" );
//		propertyAttribute = property.getMetaAttribute( "globalmutated" );
//
//		assertNotNull(propertyAttribute);
//		/*assertEquals( propertyAttribute.getValues().size(), 4 );
//		assertEquals( "top level", propertyAttribute.getValues().get(0) );
//		assertEquals( "wicked level", propertyAttribute.getValues().get(1) );
//		assertEquals( "monetaryamount anotherSet composite level", propertyAttribute.getValues().get(2) );*/
//		assertEquals( "monetaryamount anotherSet composite property empinone level", propertyAttribute.getValue() );
//
		
	}

}
