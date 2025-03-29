/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.hbm.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.spi.XmlMappingBinderAccess;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.util.ServiceRegistryUtil;

/**
 * @author Jean-François Boeuf
 */
public class XmlBindingChecker {

	public static void checkValidGeneration(JaxbHbmHibernateMapping hbmMapping)
			throws Exception {
		JAXBContext jaxbContext = JAXBContext
				.newInstance( JaxbHbmHibernateMapping.class );

		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
		jaxbMarshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		jaxbMarshaller.marshal( hbmMapping, bos );
		ByteArrayInputStream is = new ByteArrayInputStream( bos.toByteArray() );
		try (ServiceRegistry sr = ServiceRegistryUtil.serviceRegistry()) {
			new XmlMappingBinderAccess( sr ).bind( is );
		}
	}
}
