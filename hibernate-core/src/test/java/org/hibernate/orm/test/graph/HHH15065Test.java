/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.hibernate.graph.RootGraph;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@JiraKey(value = "HHH-15065")
@DomainModel(
	annotatedClasses = {
		HHH15065Test.Book.class,
		HHH15065Test.Person.class,
	}
)
@SessionFactory(useCollectingStatementInspector = true)
class HHH15065Test {

	@Test
	void testDeterministicStatementWithEntityGraphHavingMultipleAttributes(SessionFactoryScope scope) throws Exception {
		scope.inSession( session -> {
			RootGraph<Book> entityGraph = session.createEntityGraph( Book.class );
			entityGraph.addAttributeNodes( "author", "coAuthor", "editor", "coEditor" );

			session.createQuery( "select book from Book book", Book.class )
				.setHint( "javax.persistence.fetchgraph", entityGraph )
				.getResultList();
		} );

		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		List<String> sqlQueries = statementInspector.getSqlQueries();
		assertEquals( 1, sqlQueries.size() );
		assertEquals( "select b1_0.id,a1_0.id,a1_0.name,ca1_0.id,ca1_0.name,ce1_0.id,ce1_0.name,e1_0.id,e1_0.name" +
					" from Book b1_0" +
					" left join Person a1_0 on a1_0.id=b1_0.author_id" +
					" left join Person ca1_0 on ca1_0.id=b1_0.coAuthor_id" +
					" left join Person ce1_0 on ce1_0.id=b1_0.coEditor_id" +
					" left join Person e1_0 on e1_0.id=b1_0.editor_id", sqlQueries.get(0) );
	}

	@Entity(name = "Book")
	public static class Book {
		@Id
		Long id;

		@ManyToOne
		Person author;

		@ManyToOne
		Person coAuthor;

		@ManyToOne
		Person editor;

		@ManyToOne
		Person coEditor;
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		Long id;
		String name;
	}

}
