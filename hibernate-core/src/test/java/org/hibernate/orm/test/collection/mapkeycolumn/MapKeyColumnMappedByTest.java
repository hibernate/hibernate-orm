/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.mapkeycolumn;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = {Chapter.class, Book.class},
		useCollectingStatementInspector = true)
public class MapKeyColumnMappedByTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			Book book = new Book( "XXX" );
			em.persist( book );
			book.chapters.put( "one", new Chapter("XXX", "one", "Lorem ipsum") );
		} );
		scope.inTransaction( em -> {
			Book book = em.find( Book.class, "XXX" );
			book.chapters.put( "two", new Chapter("XXX", "two", "Lorem ipsum") );
		} );
		List<String> queries = ((SQLStatementInspector) scope.getStatementInspector()).getSqlQueries();
		assertEquals( 5, queries.size() ); // we can't put in a Map without initializing it
		scope.inTransaction( em -> {
			assertEquals(  em.find( Book.class, "XXX" ).chapters.size(), 2 );
		} );
	}
}
