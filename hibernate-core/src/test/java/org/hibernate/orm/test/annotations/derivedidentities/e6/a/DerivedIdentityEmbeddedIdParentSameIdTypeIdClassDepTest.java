/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e6.a;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				MedicalHistory.class,
				Person.class
		}
)
@SessionFactory
public class DerivedIdentityEmbeddedIdParentSameIdTypeIdClassDepTest {

	@Test
	public void testOneToOneExplicitJoinColumn(SessionFactoryScope scope) {
		final MetadataImplementor metadataImplementor = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", metadataImplementor ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", metadataImplementor ) );
		assertTrue( !SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", metadataImplementor ) );


		scope.inTransaction(
				session -> {
					Person e = new Person();
					e.id = new PersonId();
					e.id.firstName = "Emmanuel";
					e.id.lastName = "Bernard";
					session.persist( e );
					MedicalHistory d = new MedicalHistory();
					d.patient = e;
					session.persist( d );
					session.flush();
					session.clear();

					PersonId pId = new PersonId();
					pId.firstName = e.id.firstName;
					pId.lastName = e.id.lastName;

					d = session.get( MedicalHistory.class, pId );
					assertEquals( pId.firstName, d.patient.id.firstName );

					session.remove( d );
					session.remove( d.patient );
				}
		);
	}

	@AfterEach
	public void teardDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
