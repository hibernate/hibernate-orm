/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.binding;

import org.xml.sax.InputSource;

import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.internal.util.xml.MappingReader;
import org.hibernate.internal.util.xml.Origin;
import org.hibernate.internal.util.xml.XMLHelper;
import org.hibernate.internal.util.xml.XmlDocument;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.source.Metadata;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Basic tests of {@code hbm.xml} beinding code
 *
 * @author Steve Ebersole
 */
public class BasicHbmBindingTests extends BaseUnitTestCase {
	@Test
	public void testSuperSimpleMapping() {
		Metadata metadata = new Metadata();

		{
			XmlDocument xmlDocument = readResource( "/org/hibernate/metamodel/binding/SimpleEntity.hbm.xml" );
			metadata.getHibernateXmlBinder().bindRoot( xmlDocument );
			EntityBinding entityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
			assertNotNull( entityBinding );
			assertNotNull( entityBinding.getEntityIdentifier() );
			assertNotNull( entityBinding.getEntityIdentifier().getValueBinding() );
			assertNull( entityBinding.getVersioningValueBinding() );

			AttributeBinding idAttributeBinding = entityBinding.getAttributeBinding( "id" );
			assertNotNull( idAttributeBinding );
			assertSame( idAttributeBinding, entityBinding.getEntityIdentifier().getValueBinding() );
			assertNotNull( idAttributeBinding.getAttribute() );
			assertNotNull( idAttributeBinding.getValue() );
			assertTrue( idAttributeBinding.getValue() instanceof Column );

			AttributeBinding nameBinding = entityBinding.getAttributeBinding( "name" );
			assertNotNull( nameBinding );
			assertNotNull( nameBinding.getAttribute() );
			assertNotNull( nameBinding.getValue() );
		}
		{
			XmlDocument xmlDocument = readResource( "/org/hibernate/metamodel/binding/SimpleVersionedEntity.hbm.xml" );
			metadata.getHibernateXmlBinder().bindRoot( xmlDocument );
			EntityBinding entityBinding = metadata.getEntityBinding( SimpleVersionedEntity.class.getName() );
			assertNotNull( entityBinding );
			assertNotNull( entityBinding.getEntityIdentifier() );
			assertNotNull( entityBinding.getEntityIdentifier().getValueBinding() );
			assertNotNull( entityBinding.getVersioningValueBinding() );
			assertNotNull( entityBinding.getVersioningValueBinding().getAttribute() );

			AttributeBinding idAttributeBinding = entityBinding.getAttributeBinding( "id" );
			assertNotNull( idAttributeBinding );
			assertSame( idAttributeBinding, entityBinding.getEntityIdentifier().getValueBinding() );
			assertNotNull( idAttributeBinding.getAttribute() );
			assertNotNull( idAttributeBinding.getValue() );
			assertTrue( idAttributeBinding.getValue() instanceof Column );

			AttributeBinding nameBinding = entityBinding.getAttributeBinding( "name" );
			assertNotNull( nameBinding );
			assertNotNull( nameBinding.getAttribute() );
			assertNotNull( nameBinding.getValue() );
		}
	}

	private XmlDocument readResource(final String name) {
		final String path = "/org/hibernate/test/id/Car.hbm.xml";
		Origin origin = new Origin() {
			@Override
			public String getType() {
				return "resource";
			}

			@Override
			public String getName() {
				return name;
			}
		};
		InputSource inputSource = new InputSource( ConfigHelper.getResourceAsStream( name ) );
		return MappingReader.INSTANCE.readMappingDocument( XMLHelper.DEFAULT_DTD_RESOLVER, inputSource, origin );
	}
}
