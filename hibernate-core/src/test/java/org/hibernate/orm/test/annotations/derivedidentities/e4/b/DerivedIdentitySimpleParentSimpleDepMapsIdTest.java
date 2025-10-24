/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e4.b;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Emmanuel Bernard
 */
@SessionFactory
@DomainModel(
		annotatedClasses = {
				MedicalHistory.class,
				Person.class,
				FinancialHistory.class
		}
)
public class DerivedIdentitySimpleParentSimpleDepMapsIdTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testOneToOneExplicitJoinColumn(SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();

		assertThat( SchemaUtil.isColumnPresent( "MedicalHistory", "FK", metadata ) ).isTrue();
		assertThat( !SchemaUtil.isColumnPresent( "MedicalHistory", "id", metadata ) ).isTrue();
		Person e = new Person();
		e.ssn = "aaa";
		scope.inTransaction(
				session -> {
					session.persist( e );
					MedicalHistory d = new MedicalHistory();
					d.patient = e;
					//d.id = "aaa"; //FIXME not needed when foreign is enabled
					session.persist( d );
					session.flush();
					session.clear();
					d = session.find( MedicalHistory.class, d.id );
					assertThat( d.patient.ssn ).isEqualTo( d.id );
					d.lastupdate = new Date();
					session.flush();
					session.clear();
					d = session.find( MedicalHistory.class, d.id );
					assertThat( d.lastupdate ).isNotNull();
				}
		);
	}

	@Test
	public void testManyToOneExplicitJoinColumn(SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();

		assertThat( SchemaUtil.isColumnPresent( "FinancialHistory", "FK", metadata ) ).isTrue();
		assertThat( !SchemaUtil.isColumnPresent( "FinancialHistory", "id", metadata ) ).isTrue();
		Person e = new Person();
		e.ssn = "aaa";
		scope.inTransaction(
				session -> {
					session.persist( e );
					FinancialHistory d = new FinancialHistory();
					d.patient = e;
					//d.id = "aaa"; //FIXME not needed when foreign is enabled
					session.persist( d );
					session.flush();
					session.clear();
					d = session.find( FinancialHistory.class, d.id );
					assertThat( d.patient.ssn ).isEqualTo( d.id );
					d.lastupdate = new Date();
					session.flush();
					session.clear();
					d = session.find( FinancialHistory.class, d.id );
					assertThat( d.lastupdate ).isNotNull();
				}
		);
	}

	@Test
	public void testExplicitlyAssignedDependentIdAttributeValue(SessionFactoryScope scope) {
		// even though the id is by definition generated (using the "foreign" strategy), JPA
		// still does allow manually setting the generated id attribute value which providers
		// are expected to promptly disregard :?
		Person person = new Person( "123456789" );
		MedicalHistory medicalHistory = new MedicalHistory( "987654321", person );
		scope.inTransaction(
				session -> {
					session.persist( person );
					session.persist( medicalHistory );
				}
		);
		// again, even though we specified an id value of "987654321" prior to persist,
		// Hibernate should have replaced that with the "123456789" from the associated
		// person
		assertThat( medicalHistory.patient.ssn ).isEqualTo( person.ssn );
		assertThat(  medicalHistory.patient ).isEqualTo( person );
		assertThat(  medicalHistory.id ).isEqualTo( person.ssn );

		scope.inTransaction(
				session -> {
					MedicalHistory separateMedicalHistory = session.find( MedicalHistory.class, "987654321" );
					assertThat( separateMedicalHistory ).isNull();
					// Now we should find it...
					separateMedicalHistory = session.find( MedicalHistory.class, "123456789" );
					assertThat( separateMedicalHistory ).isNotNull();
				}
		);
	}
}
