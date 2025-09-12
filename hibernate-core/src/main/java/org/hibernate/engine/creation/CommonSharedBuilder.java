/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation;

import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Incubating;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.StatelessSession;

import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * Common options for builders of {@linkplain Session stateful}
 * and {@linkplain StatelessSession stateless} sessions
 * which share state from an underlying session.
 *
 * @since 7.2
 *
 * @author Steve Ebersole
 */
@Incubating
public interface CommonSharedBuilder extends CommonBuilder {

	/**
	 * Signifies that the connection from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonSharedBuilder connection();

	/**
	 * Signifies the interceptor from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonSharedBuilder interceptor();

	/**
	 * Signifies that the SQL {@linkplain org.hibernate.resource.jdbc.spi.StatementInspector statement inspector}
	 * from the original session should be used.
	 */
	CommonSharedBuilder statementInspector();

	/**
	 * Signifies that no SQL {@linkplain org.hibernate.resource.jdbc.spi.StatementInspector statement inspector}
	 * should be used.
	 */
	@Override
	CommonSharedBuilder noStatementInspector();

	@Override
	CommonSharedBuilder interceptor(Interceptor interceptor);

	@Override
	CommonSharedBuilder noInterceptor();

	@Override
	CommonSharedBuilder statementInspector(UnaryOperator<String> operator);

	@Override
	CommonSharedBuilder readOnly(boolean readOnly);

	@Override
	CommonSharedBuilder initialCacheMode(CacheMode cacheMode);

	@Override
	CommonSharedBuilder tenantIdentifier(Object tenantIdentifier);

	@Override
	CommonBuilder connection(Connection connection);

	@Override
	CommonSharedBuilder connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode);

	@Override
	CommonSharedBuilder jdbcTimeZone(TimeZone timeZone);
}
