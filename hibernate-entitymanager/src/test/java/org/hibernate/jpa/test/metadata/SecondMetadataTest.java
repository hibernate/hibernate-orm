/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
