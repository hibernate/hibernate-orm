package org.hibernate.test.c3p0;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.FieldResult;
import jakarta.persistence.Id;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.SqlResultSetMappings;
import jakarta.persistence.StoredProcedureParameter;

import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(OracleDialect.class)
@TestForIssue( jiraKey = "HHH-10256" )
public class OracleSQLCallableStatementProxyTest extends
		BaseCoreFunctionalTestCase {

	protected void configure(Configuration configuration) {
		configuration.setProperty(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				C3P0ConnectionProvider.class
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Person.class,
		};
	}

	@Before
	public void init() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {

				Statement statement = null;
				try {
					statement = connection.createStatement();
					statement.executeUpdate(
						"CREATE OR REPLACE FUNCTION fn_person ( " +
						"   personId IN NUMBER) " +
						"    RETURN SYS_REFCURSOR " +
						"IS " +
						"    persons SYS_REFCURSOR; " +
						"BEGIN " +
						"   OPEN persons FOR " +
						"        SELECT " +
						"            p.id AS \"p.id\", " +
						"            p.name AS \"p.name\", " +
						"            p.nickName AS \"p.nickName\" " +
						"       FROM person p " +
						"       WHERE p.id = personId; " +
						"   RETURN persons; " +
						"END;"
					);
				}
				catch (SQLException ignore) {
				}
				finally {
					if ( statement != null ) {
						statement.close();
					}
				}
			} );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Person person1 = new Person();
			person1.setId( 1L );
			person1.setName( "John Doe" );
			person1.setNickName( "JD" );
			session.persist( person1 );
		} );
	}

	@Test
	public void testStoredProcedureOutParameter() {
		doInHibernate( this::sessionFactory, session -> {
			List<Object[]> persons = session
					.createNamedStoredProcedureQuery( "getPerson" )
					.setParameter(1, 1L)
					.getResultList();
			assertEquals(1, persons.size());
		} );
	}

	@NamedStoredProcedureQuery(
			name = "getPerson",
			procedureName = "fn_person",
			resultSetMappings = "person",
			hints = @QueryHint(name = "org.hibernate.callableFunction", value = "true"),
			parameters = @StoredProcedureParameter(type = Long.class)
	)
	@SqlResultSetMappings({
		@SqlResultSetMapping(
			name = "person",
			entities = {
				@EntityResult(
						entityClass = Person.class,
						fields = {
								@FieldResult( name = "id", column = "p.id" ),
								@FieldResult( name = "name", column = "p.name" ),
								@FieldResult( name = "nickName", column = "p.nickName" ),
						}
				)
			}
		),
	})
	@Entity(name = "Person")
	@jakarta.persistence.Table(name = "person")
	public static class Person {

		@Id
		private Long id;

		private String name;

		private String nickName;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getNickName() {
			return nickName;
		}

		public void setNickName(String nickName) {
			this.nickName = nickName;
		}
	}
}
