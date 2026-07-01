/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.integ.test;

import org.hibernate.processor.test.integ.model.Author;
import org.hibernate.processor.test.integ.repository.AuthorRepository;
import org.hibernate.processor.test.integ.repository._AuthorRepository;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(annotatedClasses = { Author.class, AuthorRepository.class })
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20509")
class AuthorRepositoryTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testSaveNewAuthor(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _AuthorRepository( session );
			Author author = new Author( "Alice" );
			repo.save( author );
			assertNotNull( author.getId(), "ID should be generated after save" );
			Author found = repo.byId( author.getId() );
			assertNotNull( found );
			assertEquals( "Alice", found.getName() );
		} );
	}

	@Test
	void testSaveAllNewAuthors(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _AuthorRepository( session );
			List<Author> authors = List.of(
					new Author( "Bob" ),
					new Author( "Carol" )
			);
			repo.saveAll( authors );
			for ( Author author : authors ) {
				assertNotNull( author.getId(), "ID should be generated after saveAll" );
				Author found = repo.byId( author.getId() );
				assertNotNull( found );
			}
		} );
	}

	@Test
	void testSaveExistingAuthor(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> {
			var repo = new _AuthorRepository( session );
			Author author = new Author( "Dave" );
			repo.insert( author );
			assertNotNull( author.getId() );
			author.setName( "David" );
			repo.save( author );
			Author found = repo.byId( author.getId() );
			assertEquals( "David", found.getName() );
		} );
	}
}
