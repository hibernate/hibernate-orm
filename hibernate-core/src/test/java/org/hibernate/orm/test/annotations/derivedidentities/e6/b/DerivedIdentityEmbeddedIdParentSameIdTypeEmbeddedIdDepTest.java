/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e6.b;

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
public class DerivedIdentityEmbeddedIdParentSameIdTypeEmbeddedIdDepTest {

	@Test
	public void testOneToOneExplicitJoinColumn(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();

		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", metadata ) );


		scope.inTransaction(
				session -> {
					Person e = new Person();
					e.id = new PersonId();
					e.id.firstName = "Emmanuel";
					e.id.lastName = "Bernard";
					session.persist( e );

					MedicalHistory d = new MedicalHistory();
//		d.id = new PersonId();
//		d.id.firstName = "Emmanuel"; //FIXME not needed when foreign is enabled
//		d.id.lastName = "Bernard"; //FIXME not needed when foreign is enabled
					d.patient = e;
					session.persist( d );

					session.flush();
					session.clear();

					d = session.get( MedicalHistory.class, d.id );
					assertEquals( d.id.firstName, d.patient.id.firstName );
				}
		);
	}

	@AfterEach
	public void teardDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
