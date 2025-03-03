/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.validation;

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
@SuppressWarnings("nullness")
class MockJdbcServicesInitiator extends JdbcServicesInitiator {

	static final JdbcServicesInitiator INSTANCE = new MockJdbcServicesInitiator();

	static final JdbcServices jdbcServices = Mocker.nullary(MockJdbcServices.class).get();
	static final GenericDialect genericDialect = new GenericDialect();

	public abstract static class MockJdbcServices implements JdbcServices, JdbcEnvironment {
		@Override
		public Dialect getDialect() {
			return genericDialect;
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
			return new QualifiedObjectNameFormatterStandardImpl(getNameQualifierSupport());
		}

		@Override
		public NameQualifierSupport getNameQualifierSupport() {
			return genericDialect.getNameQualifierSupport();
		}
	}

	@Override
	public JdbcServices initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return jdbcServices;
	}
}
