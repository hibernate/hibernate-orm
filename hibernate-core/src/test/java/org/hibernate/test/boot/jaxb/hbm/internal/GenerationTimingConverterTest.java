/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.boot.jaxb.hbm.internal;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmBasicAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Jean-François Boeuf
 */
public class GenerationTimingConverterTest extends BaseUnitTestCase {

	@Test
	public void testMashallAttributeWithNullGenerationTiming()
			throws Exception {
		JaxbHbmHibernateMapping hm = new JaxbHbmHibernateMapping();
		JaxbHbmRootEntityType clazz = new JaxbHbmRootEntityType();
		JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		JaxbHbmBasicAttributeType att = new JaxbHbmBasicAttributeType();
		att.setName( "attributeName" );
		clazz.getAttributes().add( att );
		clazz.setId( id );
		hm.getClazz().add( clazz );

		XmlBindingChecker.checkValidGeneration( hm );
	}

}
