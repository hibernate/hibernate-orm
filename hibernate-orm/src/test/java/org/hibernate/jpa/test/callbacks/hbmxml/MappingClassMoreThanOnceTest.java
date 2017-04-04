/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.callbacks.hbmxml;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class MappingClassMoreThanOnceTest extends BaseUnitTestCase {
	/**
	 * Tests that an entity manager can be created when a class is mapped more than once.
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-8775")
//	@FailureExpected(jiraKey = "HHH-8775")
	public void testBootstrapWithClassMappedMOreThanOnce() {
		Map settings = new HashMap(  );
		settings.put( AvailableSettings.HBXML_FILES, "org/hibernate/jpa/test/callbacks/hbmxml/ClassMappedMoreThanOnce.hbm.xml" );

		final EntityManagerFactoryBuilder builder = Bootstrap.getEntityManagerFactoryBuilder(
				new BaseEntityManagerFunctionalTestCase.TestingPersistenceUnitDescriptorImpl( getClass().getSimpleName() ),
				settings
		);

		HibernateEntityManagerFactory emf = null;
		try {
			emf = builder.build().unwrap( HibernateEntityManagerFactory.class );
		}
		finally {
			if ( emf != null ) {
				try {
					emf.close();
				}
				catch (Exception ignore) {
				}
			}
		}
	}

}
