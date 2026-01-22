/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.options;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.SchemaValidationException;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;
import static org.hibernate.cfg.JdbcSettings.HIGHLIGHT_SQL;
import static org.hibernate.cfg.JdbcSettings.LOG_SLOW_QUERY;
import static org.hibernate.cfg.JdbcSettings.SHOW_SQL;
import static org.hibernate.internal.util.config.ConfigurationHelper.getBoolean;
import static org.hibernate.internal.util.config.ConfigurationHelper.getLong;

@DomainModel(annotatedClasses = OptionsTest.WithOptions.class)
@SessionFactory
@ServiceRegistry(serviceContributors = OptionsTest.CollectingSqlStatementLoggerServiceContributor.class)
@RequiresDialect(value = PostgreSQLDialect.class, majorVersion = 14)
public class OptionsTest {
	@Test void test(SessionFactoryScope scope) throws SchemaValidationException {
		final CollectingSqlStatementLogger statementInspector =
				(CollectingSqlStatementLogger) scope.getSessionFactory().getJdbcServices().getSqlStatementLogger();
		final SchemaManager schemaManager = scope.getSessionFactory().getSchemaManager();
		schemaManager.drop(true);
		statementInspector.clear();

		schemaManager.create(true);
		assertThat( statementInspector.getStatements().size() ).isEqualTo( 3 );
		assertThat( statementInspector.getStatements().get( 0 ) ).contains( " compression pglz" );
		assertThat( statementInspector.getStatements().get( 0 ) ).contains( " deferrable" );
		assertThat( statementInspector.getStatements().get( 1 ) ).contains( " nulls distinct" );
		assertThat( statementInspector.getStatements().get( 2 ) ).contains( " match full" );

		schemaManager.validate();
	}

	@Entity
	@Table(name = "TableWithOptions",
			indexes = @Index(columnList = "name", options = "nulls distinct"),
			uniqueConstraints = @UniqueConstraint(columnNames = "name", options = "deferrable"))
	static class WithOptions {
		@Id
		long id;

		@Column(name = "name", options = "compression pglz")
		String name;

		@ManyToOne
		@JoinColumn(foreignKey = @ForeignKey(name = "ToOther", options = "match full"))
		WithOptions other;
	}

	public static class CollectingSqlStatementLogger extends SqlStatementLogger {

		private final List<String> statements = new ArrayList<>();

		public CollectingSqlStatementLogger(boolean logToStdout, boolean format, boolean highlight, long logSlowQuery) {
			super( logToStdout, format, highlight, logSlowQuery );
		}

		@Override
		public void logStatement(String statement, Formatter formatter) {
			statements.add( statement );
			super.logStatement( statement, formatter );
		}

		public List<String> getStatements() {
			return statements;
		}

		public void clear() {
			statements.clear();
		}
	}

	public static class CollectingSqlStatementLoggerServiceContributor implements ServiceContributor {
		@Override
		public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder) {
			final Map<String, Object> configValues = serviceRegistryBuilder.getSettings();
			final boolean showSQL = getBoolean( SHOW_SQL, configValues );
			final boolean formatSQL = getBoolean( FORMAT_SQL, configValues );
			final boolean highlightSQL = getBoolean( HIGHLIGHT_SQL, configValues );
			final long logSlowQuery = getLong( LOG_SLOW_QUERY, configValues, -2 );

			serviceRegistryBuilder.addService(
					SqlStatementLogger.class,
					new CollectingSqlStatementLogger( showSQL, formatSQL, highlightSQL, logSlowQuery )
			);
		}
	}
}
