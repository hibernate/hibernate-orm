/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e5.b;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
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
public class DerivedIdentityIdClassParentSameIdTypeEmbeddedIdDepTest {

	@Test
	public void testOneToOneExplicitJoinColumn(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", metadata ) );

		Person e = new Person();
		final String firstName = "Emmanuel";
		final String lastName = "Bernard";
		e.firstName = firstName;
		e.lastName = lastName;

		scope.inTransaction(
				session -> {
					session.persist( e );
					MedicalHistory d = new MedicalHistory();
					d.patient = e;
					session.persist( d );
					session.flush();
					session.clear();
					d = session.get( MedicalHistory.class, d.id );
					assertEquals( d.id.firstName, d.patient.firstName );
				}
		);

		scope.inTransaction(
				session -> {
					PersonId pId = new PersonId( firstName, lastName );
					MedicalHistory d2 = session.get( MedicalHistory.class, pId );
					Person p2 = session.get( Person.class, pId );
					assertEquals( pId.firstName, d2.patient.firstName );
					assertEquals( pId.firstName, p2.firstName );
					session.remove( d2 );
					session.remove( p2 );
				}
		);
	}
}
