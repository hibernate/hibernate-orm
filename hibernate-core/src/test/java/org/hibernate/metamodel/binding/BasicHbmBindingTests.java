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

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.internal.util.xml.MappingReader;
import org.hibernate.internal.util.xml.Origin;
import org.hibernate.internal.util.xml.XMLHelper;
import org.hibernate.internal.util.xml.XmlDocument;
import org.hibernate.metamodel.source.Metadata;
import org.hibernate.metamodel.source.hbm.xml.mapping.HibernateMapping;
import org.hibernate.metamodel.source.util.xml.XmlHelper;

import static org.junit.Assert.fail;

/**
 * Basic tests of {@code hbm.xml} binding code
 *
 * @author Steve Ebersole
 */
public class BasicHbmBindingTests extends AbstractBasicBindingTests {
	private static final Logger log = LoggerFactory.getLogger( BasicHbmBindingTests.class );

	public EntityBinding buildSimpleEntityBinding() {
		Metadata metadata = new Metadata();

		XmlDocument xmlDocument = readResource( "/org/hibernate/metamodel/binding/SimpleEntity.hbm.xml" );
		metadata.getHibernateXmlBinder().bindRoot( xmlDocument );
		return metadata.getEntityBinding( SimpleEntity.class.getName() );
	}

	public EntityBinding buildSimpleVersionedEntityBinding() {
		Metadata metadata = new Metadata();

		String fileName = "/org/hibernate/metamodel/binding/SimpleVersionedEntity.hbm.xml";
		XmlDocument xmlDocument = readResource( fileName );
		metadata.getHibernateXmlBinder().bindRoot( xmlDocument );

		// todo - just temporary to show how things would look like with JAXB
		fileName = "/org/hibernate/metamodel/binding/SimpleVersionedEntity.xml";
		final String HIBERNATE_MAPPING_XSD = "org/hibernate/hibernate-mapping-3.0.xsd";
		HibernateMapping mapping = null;
		try {
			mapping = XmlHelper.unmarshallXml( fileName, HIBERNATE_MAPPING_XSD, HibernateMapping.class ).getRoot();
		}
		catch ( JAXBException e ) {
			log.debug( e.getMessage() );
			fail( "Unable to load xml " + fileName );
		}
		metadata.getHibernateXmlBinder().bindRoot( mapping );

		return metadata.getEntityBinding( SimpleVersionedEntity.class.getName() );
	}

	private XmlDocument readResource(final String name) {
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
