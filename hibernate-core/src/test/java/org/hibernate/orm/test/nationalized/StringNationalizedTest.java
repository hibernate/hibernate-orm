/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.nationalized;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Nationalized;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "10495")
@RequiresDialects(
		value = {
				@RequiresDialect(value = OracleDialect.class, version = 1000),
				@RequiresDialect(value = PostgreSQLDialect.class, version = 810)
		})
@DomainModel(
		annotatedClasses = StringNationalizedTest.NationalizedEntity.class
)
@SessionFactory
public class StringNationalizedTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from NationalizedEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testSaveEntityWithNationalizedProperty(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NationalizedEntity ne = new NationalizedEntity();
					ne.name = "Hello";
					session.save( ne );
				}
		);

		scope.inSession(
				session -> {
					final Query query = session.createQuery( "from NationalizedEntity where name = :name" );
					query.setParameter( "name", "Hello" );
					final List list = query.list();
					assertThat( list.size(), is( 1 ) );
				}
		);
	}

	@Entity(name = "NationalizedEntity")
	@Table(name = "NATIONALIZED_ENTITY")
	public static class NationalizedEntity {
		@Id
		@GeneratedValue
		private long id;

		@Nationalized
		String name;
	}
}
