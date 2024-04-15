package org.hibernate.test.batch;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@TestForIssue(jiraKey = "HHH-15281")
@RequiresDialect(H2Dialect.class)
public class BatchNoUseJdbcMetadataTest extends BaseCoreFunctionalTestCase {

	private final SQLStatementInspector sqlStatementInterceptor = new SQLStatementInspector();
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, "5" );
		configuration.setProperty( "hibernate.temp.use_jdbc_metadata_defaults", "false" );
		configuration.getProperties().put( Environment.STATEMENT_INSPECTOR, sqlStatementInterceptor );
	}

	@Test
	public void testBatching() {
		sqlStatementInterceptor.clear();
		inTransaction(
				session -> {
					for ( int i = 0; i < 11; i++ ) {
						Person entity = new Person();
						entity.setId( i );
						entity.setName( Integer.toString( i ) );
						session.persist( entity );
					}
				}
		);
		sqlStatementInterceptor.assertExecutedCount( 1 );
		inSession(
				session ->
						assertThat( session.getConfiguredJdbcBatchSize() ).isEqualTo( 5 )
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer oid) {
			this.id = oid;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
