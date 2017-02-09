package org.hibernate.test.c3p0;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityResult;
import javax.persistence.FieldResult;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;

import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(HANAColumnStoreDialect.class)
@TestForIssue( jiraKey = "HHH-10256" )
public class HanaSQLCallableStatementProxyTest extends
		BaseCoreFunctionalTestCase {

	protected void configure(Configuration configuration) {
		configuration.setProperty(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				C3P0ConnectionProvider.class.getName()
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
						"CREATE TABLE person ( " +
						"id LONG," +
						"name VARCHAR(64)," +
						"nickName VARCHAR(64)" +
						");"
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
					.createNamedQuery(
							"getPerson")
					.setParameter(1, 1L)
					.getResultList();
			assertEquals(1, persons.size());
		} );
	}

	@NamedNativeQuery(
			name = "getPerson",
			query = "select p.id, p.name, p.nickName from person p where p.id = ?1",
			callable = false,
			resultSetMapping = "person"
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
	@javax.persistence.Table(name = "person")
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
