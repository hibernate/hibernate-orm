package org.hibernate.processor.validation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.annotations.processing.GenericDialect;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.internal.QualifiedObjectNameFormatterStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.QualifiedObjectNameFormatter;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;

import java.util.Map;

/**
 * @author Gavin King
 */
@SuppressWarnings("NullAway")
class MockJdbcServicesInitiator extends JdbcServicesInitiator {

	private static final GenericDialect GENERIC_DIALECT = new GenericDialect();

	private final JdbcServices jdbcServices;

	public MockJdbcServicesInitiator(@Nullable Dialect dialect) {
		this.jdbcServices = Mocker.variadic(MockJdbcServices.class).make( dialect == null ? GENERIC_DIALECT : dialect );
	}

	public abstract static class MockJdbcServices implements JdbcServices, JdbcEnvironment {

		private final @Nonnull Dialect dialect;

		public MockJdbcServices(@Nonnull Dialect dialect) {
			this.dialect = dialect;
		}

		@Override
		@Nonnull
		public Dialect getDialect() {
			return dialect;
		}

		@Override
		@Nonnull
		public JdbcEnvironment getJdbcEnvironment() {
			return this;
		}

		@Override
		@Nonnull
		public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
			return new StandardSqlAstTranslatorFactory();
		}

		@Override
		@Nullable
		public Identifier getCurrentCatalog() {
			return null;
		}

		@Override
		@Nullable
		public Identifier getCurrentSchema() {
			return null;
		}

		@Override
		@Nonnull
		public QualifiedObjectNameFormatter getQualifiedObjectNameFormatter() {
			return new QualifiedObjectNameFormatterStandardImpl(getNameQualifierSupport(), ".");
		}

		@Override
		@Nonnull
		public NameQualifierSupport getNameQualifierSupport() {
			final var support = dialect.getNameQualifierSupport();
			return support == null ? NameQualifierSupport.BOTH : support;
		}
	}

	@Override
	public JdbcServices initiateService(@Nonnull Map configurationValues, @Nonnull ServiceRegistryImplementor registry) {
		return jdbcServices;
	}
}
