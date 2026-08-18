/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;

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

		private final Dialect dialect;

		public MockJdbcServices(Dialect dialect) {
			this.dialect = dialect;
		}

		@Override
		public Dialect getDialect() {
			return dialect;
		}

		@Override
		public JdbcEnvironment getJdbcEnvironment() {
			return this;
		}

		@Override
		public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
			return new StandardSqlAstTranslatorFactory();
		}

		@Override
		public Identifier getCurrentCatalog() {
			return null;
		}

		@Override
		public Identifier getCurrentSchema() {
			return null;
		}

		@Override
		public QualifiedObjectNameFormatter getQualifiedObjectNameFormatter() {
			return new QualifiedObjectNameFormatterStandardImpl(getNameQualifierSupport(), ".");
		}

		@Override
		public NameQualifierSupport getNameQualifierSupport() {
			return dialect.getNameQualifierSupport();
		}
	}

	@Override
	public JdbcServices initiateService(@Nonnull Map configurationValues, @Nonnull ServiceRegistryImplementor registry) {
		return jdbcServices;
	}
}
