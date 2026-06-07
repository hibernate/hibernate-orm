/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.reactive;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@CompilationTest
class ReactiveTest {
	@Test
	@WithClasses({ Publisher.class, Author.class, Address.class, Book.class, Library.class, Library2.class, RepoWithPrimary.class })
	void test() {
		final String library = getMetaModelSourceAsString( Library.class, true );
		final String library2 = getMetaModelSourceAsString( Library2.class, true );
		final String libraryQueryMetamodel = getMetaModelSourceAsString( Library.class );
		final String library2QueryMetamodel = getMetaModelSourceAsString( Library2.class );
		System.out.println( getMetaModelSourceAsString( Author.class ) );
		System.out.println( getMetaModelSourceAsString( Book.class ) );
		System.out.println( getMetaModelSourceAsString( Author.class, true ) );
		System.out.println( getMetaModelSourceAsString( Book.class, true ) );
		System.out.println( library );
		System.out.println( library2 );
		System.out.println( libraryQueryMetamodel );
		System.out.println( library2QueryMetamodel );
		assertTrue( library.contains( "@Nonnull\n\tpublic Uni<Void> create(@Nonnull Book book)" ) );
		assertTrue( library.contains( "@Nonnull\n\tpublic Uni<Publisher> save(@Nonnull Publisher publisher)" ) );
		assertTrue( library2.contains( "@Nonnull\n\tpublic Uni<Void> deleteAll(@Nonnull List<Publisher> publishers)" ) );
		assertTrue( libraryQueryMetamodel.contains( "TypedQueryReference<Book> booksByTitle(String titlePattern)" ) );
		assertTrue( libraryQueryMetamodel.contains( "TypedQueryReference<BookWithAuthor> booksWithAuthors()" ) );
		assertTrue( libraryQueryMetamodel.contains( "StatementReference updateAuthorAddress1(String id, Address address)" ) );
		assertTrue( libraryQueryMetamodel.contains( "StatementReference updateAuthorAddress2(String id, Address address)" ) );
		assertTrue( libraryQueryMetamodel.contains( "StatementReference updateAuthorAddress3(String id, Address address)" ) );
		assertTrue( library.contains( "SelectionSpecification.create(Library_.booksByTitle(titlePattern))" ) );
		assertTrue( library.contains( "session.createQuery(Library_.booksWithAuthors())" ) );
		assertTrue( library.contains( "session.createNamedQuery(Library_.updateAuthorAddress1(id, address).getName())" ) );
		assertTrue( library.contains( "session.createNamedQuery(Library_.updateAuthorAddress2(id, address).getName())" ) );
		assertTrue( library.contains( "session.createNamedQuery(Library_.updateAuthorAddress3(id, address).getName())" ) );
		assertFalse( library.contains( "SelectionSpecification.create(new StaticTypedQueryReference<>(" ) );
		assertFalse( library.contains( "createNamedQuery(\"org.hibernate.processor.test.data.reactive.Library#booksWithAuthors()" ) );
		assertFalse( library.contains( "createNamedQuery(\"org.hibernate.processor.test.data.reactive.Library#updateAuthorAddress" ) );
		assertTrue( library2QueryMetamodel.contains( "TypedQueryReference<Book> booksByTitle(String titlePattern)" ) );
		assertTrue( library2QueryMetamodel.contains( "TypedQueryReference<BookWithAuthor> booksWithAuthors()" ) );
		assertTrue( library2.contains( "SelectionSpecification.create(Library2_.booksByTitle(titlePattern))" ) );
		assertTrue( library2.contains( "session.createQuery(Library2_.booksWithAuthors())" ) );
		assertFalse( library2.contains( "SelectionSpecification.create(new StaticTypedQueryReference<>(" ) );
		assertFalse( library2.contains( "createNamedQuery(\"org.hibernate.processor.test.data.reactive.Library2#booksWithAuthors()" ) );
		assertMetamodelClassGeneratedFor( Author.class, true );
		assertMetamodelClassGeneratedFor( Book.class, true );
		assertMetamodelClassGeneratedFor( Publisher.class, true );
		assertMetamodelClassGeneratedFor( Author.class );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( Publisher.class );
		assertMetamodelClassGeneratedFor( Library.class );
		assertMetamodelClassGeneratedFor( Library2.class );
		assertMetamodelClassGeneratedFor( Library.class, true );
		assertMetamodelClassGeneratedFor( Library2.class, true );
	}
}
