/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.mutationquery;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

@SessionFactory
@DomainModel(annotatedClasses = {
		MutationQueriesCollectionTableTest.Base1.class,
		MutationQueriesCollectionTableTest.Table1.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-19740")
public class MutationQueriesCollectionTableTest {

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from Table1 where name = :name" )
					.setParameter( "name", "test" )
					.executeUpdate();
		} );
	}

	@Entity(name = "Base1")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public abstract static class Base1 {

		@Id
		private Long id;

		@SuppressWarnings("unused")
		private String name;

		@ElementCollection
		private List<String> roles;

	}

	@Entity(name = "Table1")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class Table1 extends Base1 {

	}
}
