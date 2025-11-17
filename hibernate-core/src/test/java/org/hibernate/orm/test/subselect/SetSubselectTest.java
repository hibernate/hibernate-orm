/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/subselect/Book.hbm.xml")
@SessionFactory
public class SetSubselectTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testSubselect(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( session -> {
			Author b = new Author();
			b.setName( "Camilleri" );
			b.setId( 1 );
			session.persist( b );

			Book book = new Book();
			book.setId( 2 );
			book.setAuthorId( 1 );
			book.setTitle( "Il sognaglio" );
			session.persist( book );

			Book book2 = new Book();
			book2.setId( 3 );
			book2.setAuthorId( 1 );
			book2.setTitle( "Il casellante" );
			session.persist( book2 );
		} );

		factoryScope.inTransaction( session -> {
			Author author = session.find( Author.class, 1 );
			assertThat( author.getBooks().size(), is( 2 ) );
		} );
	}

}
