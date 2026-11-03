/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import org.hibernate.cfg.MappingSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@ServiceRegistry(settings = @Setting(name = MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true"))
@DomainModel(annotatedClasses = CteGloballyQuotedIdentifiersTest.Book.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-21056")
public class CteGloballyQuotedIdentifiersTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testCteWithoutAlias(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Book book = new Book();
			book.id = 1L;
			session.persist( book );
		} );
		scope.inTransaction( session ->
				assertThat( session.createQuery( "with cte as (select b.id as id from Book b) select id from cte", Long.class )
						.getResultList() ).containsExactly( 1L )
		);
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		Long id;
		String name;
	}
}
