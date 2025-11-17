/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metadata;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * @author Emmanuel Bernard
 */
@Jpa(
		annotatedClasses = {
				DeskWithRawType.class,
				EmployeeWithRawType.class,
				SimpleMedicalHistory.class,
				SimplePerson.class
		}
)
public class SecondMetadataTest {

	@Test
	public void testBaseOfService(EntityManagerFactoryScope scope) {
		EntityManagerFactory emf = scope.getEntityManagerFactory();
		assertNotNull( emf.getMetamodel() );
		assertNotNull( emf.getMetamodel().entity( DeskWithRawType.class ) );
		assertNotNull( emf.getMetamodel().entity( EmployeeWithRawType.class ) );
		assertNotNull( emf.getMetamodel().entity( SimpleMedicalHistory.class ) );
		assertNotNull( emf.getMetamodel().entity( SimplePerson.class ) );
	}
}
