/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.boot.jaxb.hbm.internal;

import org.hibernate.EntityMode;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;
import org.hibernate.tuple.entity.DynamicMapEntityTuplizer;

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

	private JaxbHbmHibernateMapping generateXml(boolean includeEntityMode)
			throws Exception {
		JaxbHbmHibernateMapping hm = new JaxbHbmHibernateMapping();
		JaxbHbmRootEntityType clazz = new JaxbHbmRootEntityType();
		JaxbHbmTuplizerType tuplizer = new JaxbHbmTuplizerType();
		tuplizer.setClazz( DynamicMapEntityTuplizer.class.getCanonicalName() );
		if ( includeEntityMode ) {
			tuplizer.setEntityMode( EntityMode.MAP );
		}
		clazz.getTuplizer().add( tuplizer );
		JaxbHbmSimpleIdType id = new JaxbHbmSimpleIdType();
		clazz.setId( id );
		hm.getClazz().add( clazz );
		return hm;
	}
}
