/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph;

import static org.junit.Assert.*;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Session;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-15065")
public class HHH15065Test extends BaseNonConfigCoreFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			HHH15065Test.Book.class,
			HHH15065Test.Person.class
		};
	}

	@Test
	public void testDeterministicStatementWithEntityGraphHavingMultipleAttributes() throws Exception {
		final Session session = openSession();
		session.beginTransaction();

		RootGraph<Book> entityGraph = session.createEntityGraph(Book.class);
		entityGraph.addAttributeNodes( "author", "coAuthor", "editor", "coEditor" );

		session.createQuery( "select book from Book book", Book.class )
			.setHint( "javax.persistence.fetchgraph", entityGraph )
			.getResultList();

		List<String> sqlQueries = sqlStatementInterceptor.getSqlQueries();

		assertEquals( 1, sqlQueries.size() );
		assertEquals( "select hhh15065te0_.id as id1_0_0_," +
					  " hhh15065te1_.id as id1_1_1_," +
					  " hhh15065te2_.id as id1_1_2_," +
					  " hhh15065te3_.id as id1_1_3_," +
					  " hhh15065te4_.id as id1_1_4_," +
					  " hhh15065te0_.author_id as author_i2_0_0_," +
					  " hhh15065te0_.coAuthor_id as coauthor3_0_0_," +
					  " hhh15065te0_.coEditor_id as coeditor4_0_0_," +
					  " hhh15065te0_.editor_id as editor_i5_0_0_" +
					  " from Book hhh15065te0_" +
					  " left outer join Person hhh15065te1_ on hhh15065te0_.author_id=hhh15065te1_.id" +
					  " left outer join Person hhh15065te2_ on hhh15065te0_.coAuthor_id=hhh15065te2_.id" +
					  " left outer join Person hhh15065te3_ on hhh15065te0_.editor_id=hhh15065te3_.id" +
					  " left outer join Person hhh15065te4_ on hhh15065te0_.coEditor_id=hhh15065te4_.id",
			sqlQueries.get(0) );

		session.close();
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
	}

}
