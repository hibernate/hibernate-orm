/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.boot;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpa.test.jee.OrmVersionTest;
import org.hibernate.jpa.HibernatePersistenceProvider;

import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class NewBootProcessTest {
	@Test
	public void basicNewBootProcessTest() {
		Map settings = new HashMap();

		HibernatePersistenceProvider persistenceProvider = new HibernatePersistenceProvider();
		persistenceProvider.createContainerEntityManagerFactory(
				new OrmVersionTest.PersistenceUnitInfoImpl( "my-test" ) {
					@Override
					public URL getPersistenceUnitRootUrl() {
						// just get any known url...
						return HibernatePersistenceProvider.class.getResource( "/org/hibernate/jpa/persistence_1_0.xsd" );
					}
				},
				settings
		);
	}
}
