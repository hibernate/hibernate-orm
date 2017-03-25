/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.boot.jaxb.hbm.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.XmlMappingBinderAccess;
import org.hibernate.service.ServiceRegistry;

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
		ServiceRegistry sr = new StandardServiceRegistryBuilder().build();
		new XmlMappingBinderAccess( sr ).bind( is );
	}
}
