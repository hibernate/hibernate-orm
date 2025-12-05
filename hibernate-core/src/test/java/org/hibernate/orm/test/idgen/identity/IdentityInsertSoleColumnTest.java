/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.identity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.dialect.HANADialect;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * The purpose of this test is to insure that when an entity that contains a single column
 * which also happens to be an identity-generated identifier, the generated insert SQL is
 * correct for the dialect.
 *
 * We found through research that SAP Hana doesn't support an empty values-list clause, the
 * omission of the values-list clause, or any default value in the values-list clause for
 * the identifier column; therefore we'll skip this test for that platform.
 *
 * @author Chris Cranford
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-13104")
@RequiresDialectFeature(feature= DialectFeatureChecks.SupportsIdentityColumns.class)
@SkipForDialect(dialectClass=HANADialect.class,
		reason="SAP HANA requires at least value in insert value-list clause.")
@DomainModel(annotatedClasses = IdentityInsertSoleColumnTest.Animal.class)
@SessionFactory
public class IdentityInsertSoleColumnTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testEntityInsertWithSingleIdentityAttributeColumn(SessionFactoryScope factoryScope) {
		// insert the entity
		final Integer entityId = factoryScope.fromTransaction( (entityManager) -> {
			var animal = new Animal();
			entityManager.persist( animal );
			return animal.getId();
		} );

		// make sure the identifier was generated
		assertThat( entityId ).isNotNull();

		// verify the entity can be fetched
		factoryScope.inTransaction(  (entityManager) -> {
			var animal = entityManager.find( Animal.class, entityId );
			assertThat( animal ).isNotNull();
		} );
	}

	@Entity(name = "Animal")
	public static class Animal {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
