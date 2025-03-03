/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = BatchNoUseJdbcMetadataTest.Person.class
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5"),
				@Setting(name = "hibernate.temp.use_jdbc_metadata_defaults", value = "false")
		}
)
@JiraKey(value = "HHH-15281")
@RequiresDialect(H2Dialect.class)
public class BatchNoUseJdbcMetadataTest {

	@Test
	public void testBatching(SessionFactoryScope scope) {
		final SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		sqlStatementInterceptor.clear();
		scope.inTransaction(
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
		scope.inSession(
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
