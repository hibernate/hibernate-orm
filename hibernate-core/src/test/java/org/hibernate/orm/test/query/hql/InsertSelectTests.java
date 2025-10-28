/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.community.dialect.DerbyDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ServiceRegistry
@DomainModel( annotatedClasses = {
		InsertSelectTests.EntityEntry.class,
		InsertSelectTests.EntitySource.class
})
@SessionFactory(useCollectingStatementInspector = true)
public class InsertSelectTests {

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new EntitySource( 1, "A" ) );
					session.persist( new EntitySource( 2, "A" ) );
				}
		);
	}

	@AfterEach
	public void cleanupTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey( value = "HHH-15527")
	public void testInsertSelectGeneratedAssigned(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createMutationQuery(
							"insert into EntityEntry (id, name) " +
									"select 1, 'abc' from EntityEntry e"
					).executeUpdate();
					statementInspector.assertExecutedCount( 1 );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15531")
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby doesn't really support window functions, " +
			"but this requires the use of a dense_rank window function. We could emulate this, but don't think it's worth it")
	public void testInsertSelectDistinct(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final int rows = session.createMutationQuery(
							"insert into EntityEntry (name) " +
									"select distinct e.name from EntitySource e"
					).executeUpdate();
					assertEquals( 1, rows );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15531")
	@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby doesn't really support window functions and " +
			"its attempt at a row_number function fails to deliver the desired semantics")
	public void testInsertSelectGroupBy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final int rows = session.createMutationQuery(
							"insert into EntityEntry (name) " +
									"select e.name from EntitySource e group by e.name"
					).executeUpdate();
					assertEquals( 1, rows );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-16786")
	public void testInsertSelectParameterInference(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					statementInspector.clear();
					session.createMutationQuery(
							"insert into EntityEntry (id, name, source) " +
									"select 1, 'abc', :source from EntityEntry e"
					).setParameter( "source", null ).executeUpdate();
					statementInspector.assertExecutedCount( 1 );
				}
		);
	}

	@Entity(name = "EntityEntry")
	public static class EntityEntry {
		@Id
		@GeneratedValue
		Integer id;
		String name;
		@ManyToOne
		EntitySource source;
	}

	@Entity(name = "EntitySource")
	public static class EntitySource {
		@Id
		Integer id;
		String name;

		public EntitySource() {
		}

		public EntitySource(Integer id) {
			this.id = id;
		}

		public EntitySource(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
