/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProviderSettingProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Jpa(
		standardModels = StandardDomainModel.CONTACTS,
		settingProviders = {
				@SettingProvider(settingName = AvailableSettings.CONNECTION_PROVIDER,
						provider = PreparedStatementSpyConnectionProviderSettingProvider.class)
		}
)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
public class QuerySqlExceptionTest {

	private PreparedStatementSpyConnectionProvider connectionProvider;

	@BeforeAll
	public void init(EntityManagerFactoryScope scope) {
		connectionProvider = (PreparedStatementSpyConnectionProvider) scope.getEntityManagerFactory().getProperties()
				.get( AvailableSettings.CONNECTION_PROVIDER );
	}

	@Test
	public void sqlExceptionOnExecutionWillCloseStatement(EntityManagerFactoryScope scope) {
		// We need at least one row in the "contacts" table,
		// otherwise the SELECT below might not even get executed completely
		// (the DB somehow detects the result will be 0 rows anyway and doesn't bother evaluating parameters).
		scope.inTransaction( entityManager -> {
			var contact = new Contact(
					1,
					new Contact.Name( "John", "Doe" ),
					Contact.Gender.MALE,
					LocalDate.of( 1970, 1, 1 )
			);
			entityManager.persist( contact );
		} );
		scope.inTransaction( entityManager -> {
			connectionProvider.clear();
			assertThatThrownBy( () -> createQueryTriggeringStatementExecutionFailure( entityManager ).getResultList() )
					.satisfiesAnyOf(
							// Behavior differs depending on the dialect
							e -> assertThat( e ).isInstanceOf( SQLException.class ),
							e -> assertThat( e ).hasCauseInstanceOf( SQLException.class ),
							e -> assertThat( e ).hasRootCauseInstanceOf( SQLException.class )
					);
			// Checking immediately, because the JDBC driver or connection pool might "fix" statement leaks
			// when the connection gets closed on transaction commit.
			assertThat( connectionProvider.getPreparedStatementsAndSql().entrySet() )
					.isNotEmpty()
					.allSatisfy( e -> assertThat( e.getKey().isClosed() )
							.as( "Statement '" + e.getValue() + "' is closed" )
							.isTrue() );
		} );
	}

	// Creates a query that will intentionally trigger an exception
	// during statement execution (not during preparation).
	private Query createQueryTriggeringStatementExecutionFailure(EntityManager entityManager) {
		var dialect = entityManager.getEntityManagerFactory().unwrap( SessionFactoryImplementor.class )
				.getJdbcServices().getDialect();
		Object badParamValue;
		if ( dialect instanceof MySQLDialect ) {
			// These databases are perfectly fine with the operation `"foo" / 2`
			// and will happily return `0.0` without any error...
			// Let's give them something even more nonsensical
			// (but which we cannot pass to other DBs as they would detect the problem too early)
			badParamValue = List.of( "foo", "bar" );
		}
		else {
			badParamValue = "foo";
		}
		return entityManager.createNativeQuery( "select ( :param / 2 ) from contacts" )
				.setParameter( "param", badParamValue );
	}
}
