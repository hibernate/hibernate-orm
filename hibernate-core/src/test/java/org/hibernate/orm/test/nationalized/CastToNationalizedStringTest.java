package org.hibernate.orm.test.nationalized;

import java.util.Locale;

import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				CastToNationalizedStringTest.TestEntity.class
		}
)
@RequiresDialects(
		value = {
				@RequiresDialect(value = OracleDialect.class)
		})
@SessionFactory(
		statementInspectorClass = SQLStatementInspector.class
)
@JiraKey("HHH-16521")
public class CastToNationalizedStringTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity entity = new TestEntity( 1l, "first" );
					session.persist( entity );
				}
		);
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInspector = (SQLStatementInspector) scope.getStatementInspector();
		sqlStatementInspector.clear();
		scope.inTransaction(
				session -> {
					session.createQuery( "select CAST(t.name AS nstring(200)) from TestEntity t" )
							.list();
					assertThat( sqlStatementInspector.getSqlQueries().get( 0 ).toLowerCase( Locale.ROOT ) )
							.contains( "nvarchar2" );
					sqlStatementInspector.clear();

					session.createQuery( "select CAST(t.name AS string) from TestEntity t" )
							.list();
					assertThat( sqlStatementInspector.getSqlQueries().get( 0 ).toLowerCase( Locale.ROOT ) )
							.doesNotContain( "nvarchar2" );

					session.createQuery( "select t.name from TestEntity t" )
							.list();
					assertThat( sqlStatementInspector.getSqlQueries().get( 0 ).toLowerCase( Locale.ROOT ) )
							.doesNotContain( "nvarchar2" );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		public Long id;

		public String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
