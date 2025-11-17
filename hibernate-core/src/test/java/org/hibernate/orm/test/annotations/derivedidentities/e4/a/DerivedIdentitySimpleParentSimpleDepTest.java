/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e4.a;

import java.util.Date;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				MedicalHistory.class,
				Simple.class,
				Person.class,
				FinancialHistory.class
		}
)
@SessionFactory
public class DerivedIdentitySimpleParentSimpleDepTest {

	@Test
	public void testOneToOneExplicitJoinColumn(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "MedicalHistory", "id", metadata ) );

		Person person = new Person( "aaa" );
		scope.inTransaction(
				session -> {
					session.persist( person );
					MedicalHistory medicalHistory = new MedicalHistory( person );
					session.persist( medicalHistory );
				}
		);

		scope.inTransaction(
				session -> {
					MedicalHistory medicalHistory = session.get( MedicalHistory.class, "aaa" );
					assertEquals( person.ssn, medicalHistory.patient.ssn );
					medicalHistory.lastupdate = new Date();
				}
		);
	}

	@Test
	public void testManyToOneExplicitJoinColumn(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();

		assertTrue( SchemaUtil.isColumnPresent( "FinancialHistory", "patient_ssn", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "FinancialHistory", "id", metadata ) );

		Person person = new Person( "aaa" );
		scope.inTransaction(
				session -> {
					session.persist( person );
					FinancialHistory financialHistory = new FinancialHistory( person );
					session.persist( financialHistory );
				}
		);

		scope.inTransaction(
				session -> {
					FinancialHistory financialHistory = session.get( FinancialHistory.class, "aaa" );
					assertEquals( person.ssn, financialHistory.patient.ssn );
					financialHistory.lastUpdate = new Date();
				}
		);
	}

	@Test
	public void testSimplePkValueLoading(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person e = new Person( "aaa" );
					session.persist( e );
					FinancialHistory d = new FinancialHistory( e );
					session.persist( d );
				}
		);

		scope.inTransaction(
				session -> {
					FinancialHistory history = session.get( FinancialHistory.class, "aaa" );
					assertNotNull( history );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					FinancialHistory history = session.get( FinancialHistory.class, "aaa" );
					if ( history != null ) {
						session.remove( history );
						session.remove( history.patient );
					}
				}
		);

		scope.inTransaction(
				session -> {
					MedicalHistory history = session.get( MedicalHistory.class, "aaa" );
					if ( history != null ) {
						session.remove( history );
						session.remove( history.patient );
					}
				}
		);
	}
}
