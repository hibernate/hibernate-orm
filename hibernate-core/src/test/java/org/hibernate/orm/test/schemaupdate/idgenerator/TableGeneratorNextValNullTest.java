/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.idgenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.persistence.TableGenerator;
import org.hibernate.HibernateException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JiraKey(value = "HHH-8535")
@DomainModel(annotatedClasses = {TableGeneratorNextValNullTest.Author.class})
@SessionFactory
public class TableGeneratorNextValNullTest {

	@AfterAll
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void hhh8535Test(SessionFactoryScope scope) {

		// This situation can only happen via human being or bad migration/clone script.
		// Simulate this record being updated post table generation.
		scope.inTransaction( session -> {
			session.createNativeMutationQuery(
					"UPDATE generator SET next_val = null where sequence_name = 'Author'"
			).executeUpdate();
		} );

		HibernateException hibernateException = assertThrows( HibernateException.class,
				() -> scope.inTransaction( session -> {
					Author author = new Author();
					session.persist( author );
				} ) );

		assertEquals( "next_val for sequence_name 'Author' is null", hibernateException.getMessage() );
	}

	@Entity(name = "Author")
	public static class Author {

		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = "generator")
		@TableGenerator(name = "generator", table = "generator")
		long id;
	}

}
