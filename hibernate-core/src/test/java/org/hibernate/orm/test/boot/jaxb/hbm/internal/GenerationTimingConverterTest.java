/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.hbm.internal;

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
