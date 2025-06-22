/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.ordercol;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {Page.class, Book.class},
		useCollectingStatementInspector = true)
public class OrderColumnMappedByTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Book book = new Book( "XXX" );
			em.persist( book );
			book.pages.add( new Page("XXX", 1, "Lorem ipsum") );
		} );
		scope.inTransaction( em -> {
			Book book = em.find( Book.class, "XXX" );
			book.pages.add( new Page("XXX", 2, "Lorem ipsum") );
		} );
		List<String> queries = ((SQLStatementInspector) scope.getStatementInspector()).getSqlQueries();
		assertEquals( 4, queries.size() );
		scope.inTransaction( em -> {
			assertEquals(  em.find( Book.class, "XXX" ).pages.size(), 2 );
		} );
	}
}
