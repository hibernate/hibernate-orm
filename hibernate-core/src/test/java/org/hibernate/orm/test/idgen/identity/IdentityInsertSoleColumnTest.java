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
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

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
@JiraKey(value = "HHH-13104")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
@SkipForDialect(value=HANADialect.class, comment="SAP HANA requires at least value in insert value-list clause.")
public class IdentityInsertSoleColumnTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Animal.class };
	}

	@Test
	public void testEntityInsertWithSingleIdentityAttributeColumn() {
		// insert the entity
		final Integer entityId = doInJPA( this::entityManagerFactory, entityManager -> {
			final Animal animal = new Animal();
			entityManager.persist( animal );
			return animal.getId();
		} );

		// make sure the identifier was generated
		assertNotNull( entityId );

		// verify the entity can be fetched
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Animal animal = entityManager.find( Animal.class, entityId );
			assertNotNull( animal );
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
