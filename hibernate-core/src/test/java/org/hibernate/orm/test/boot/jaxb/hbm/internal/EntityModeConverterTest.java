/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.boot.jaxb.hbm.internal;

import org.hibernate.EntityMode;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Jean-François Boeuf
 */
public class EntityModeConverterTest extends BaseUnitTestCase {

	@Test
	public void testMashallNullEntityMode() throws Exception {
		XmlBindingChecker.checkValidGeneration( generateXml( false ) );
	}

	@Test
	public void testMashallNotNullEntityMode() throws Exception {
		XmlBindingChecker.checkValidGeneration( generateXml( true ) );
	}

	private JaxbHbmHibernateMapping generateXml(boolean includeEntityMode) {
		final JaxbHbmHibernateMapping hm = new JaxbHbmHibernateMapping();

		final JaxbHbmRootEntityType clazz = new JaxbHbmRootEntityType();
		hm.getClazz().add( clazz );

		final JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		clazz.setId( id );

		if ( includeEntityMode ) {
			final JaxbHbmTuplizerType tuplizer = new JaxbHbmTuplizerType();
			clazz.getTuplizer().add( tuplizer );

			tuplizer.setEntityMode( EntityMode.MAP );
			// we don't care about the actual class.  in fact starting with 6 we completely
			// ignore this tuplizer node
			tuplizer.setClazz( "a.b.c" );
		}

		return hm;
	}
}
