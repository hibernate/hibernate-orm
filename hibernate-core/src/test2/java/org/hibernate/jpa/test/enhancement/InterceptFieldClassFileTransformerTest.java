/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.enhancement;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.Managed;
import org.hibernate.engine.spi.ManagedComposite;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.jpa.test.enhancement.cases.domain.Simple;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Dustin Schultz
 */
public class InterceptFieldClassFileTransformerTest {

	private List<String> entities = new ArrayList<String>();
	private InstrumentedClassLoader loader = null;

	@Before
	public void setup() {
		entities.add( Simple.class.getName() );
		// use custom class loader which enhances the class
		InstrumentedClassLoader cl = new InstrumentedClassLoader( Thread.currentThread().getContextClassLoader() );
		cl.setEntities( entities );
		this.loader = cl;
	}

	/**
	 * Tests that class file enhancement works.
	 *
	 * @throws Exception in case the test fails.
	 */
	@Test
	public void testEnhancement() throws Exception {
		// sanity check that the class is unmodified and does not contain getFieldHandler()
		assertFalse( implementsManaged( Simple.class ) );

		Class clazz = loader.loadClass( entities.get( 0 ) );

		// enhancement would have added the ManagedEntity interface...
		assertTrue( implementsManaged( clazz ) );
	}

	private boolean implementsManaged(Class clazz) {
		for ( Class intf : clazz.getInterfaces() ) {
			if ( Managed.class.getName().equals( intf.getName() )
					|| ManagedEntity.class.getName().equals( intf.getName() )
					|| ManagedComposite.class.getName().equals( intf.getName() ) ) {
				return true;
			}
		}
		return false;
	}
}
