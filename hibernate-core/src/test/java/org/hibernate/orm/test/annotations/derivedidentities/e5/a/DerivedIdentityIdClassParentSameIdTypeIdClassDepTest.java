/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e5.a;

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
public class DerivedIdentityIdClassParentSameIdTypeIdClassDepTest {
	private static final String FIRST_NAME = "Emmanuel";
	private static final String LAST_NAME = "Bernard";

	@Test
	public void testOneToOneExplicitJoinColumn(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", metadata ) );

		scope.inTransaction(
				session -> {
					Person e = new Person( FIRST_NAME, LAST_NAME );
					session.persist( e );
					MedicalHistory d = new MedicalHistory( e );
					session.persist( d );
					session.flush();
					session.refresh( d );
				}
		);

		scope.inTransaction(
				session -> {
					PersonId pId = new PersonId( FIRST_NAME, LAST_NAME );
					MedicalHistory d2 = session.get( MedicalHistory.class, pId );
					Person p2 = session.get( Person.class, pId );
					assertEquals( pId.firstName, d2.patient.firstName );
					assertEquals( pId.firstName, p2.firstName );
					session.remove( d2 );
					session.remove( p2 );
				}
		);
	}

	@Test
	public void testTckLikeBehavior(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();

		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", metadata ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", metadata ) );

		scope.inTransaction(
				session -> {
					Person e = new Person( FIRST_NAME, LAST_NAME );
					session.persist( e );
					MedicalHistory d = new MedicalHistory( e );
					session.persist( d );
					session.flush();
					session.refresh( d );
					session.getTransaction().commit();

					// NOTE THAT WE LEAVE THE SESSION OPEN!

					session.getTransaction().begin();
					PersonId pId = new PersonId( FIRST_NAME, LAST_NAME );
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
