/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metadata;

import javax.persistence.EntityManagerFactory;

import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
public class SecondMetadataTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testBaseOfService() throws Exception {
		EntityManagerFactory emf = entityManagerFactory();
		assertNotNull( emf.getMetamodel() );
		assertNotNull( emf.getMetamodel().entity( DeskWithRawType.class ) );
		assertNotNull( emf.getMetamodel().entity( EmployeeWithRawType.class ) );
		assertNotNull( emf.getMetamodel().entity( SimpleMedicalHistory.class ) );
		assertNotNull( emf.getMetamodel().entity( SimplePerson.class ) );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				DeskWithRawType.class,
				EmployeeWithRawType.class,
				SimpleMedicalHistory.class,
				SimplePerson.class
		};
	}
}
