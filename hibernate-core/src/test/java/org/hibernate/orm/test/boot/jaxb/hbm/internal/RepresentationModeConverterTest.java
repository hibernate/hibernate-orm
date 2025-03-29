/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.hbm.internal;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Jean-François Boeuf
 */
public class RepresentationModeConverterTest extends BaseUnitTestCase {

	@Test
	public void testMashallNullEntityMode() throws Exception {
		XmlBindingChecker.checkValidGeneration( generateXml() );
	}


	private JaxbHbmHibernateMapping generateXml()  {
		JaxbHbmHibernateMapping hm = new JaxbHbmHibernateMapping();
		JaxbHbmRootEntityType clazz = new JaxbHbmRootEntityType();
		JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		clazz.setId( id );
		hm.getClazz().add( clazz );
		return hm;
	}
}
