/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.function.UnaryOperator;

/**
 * Common options for builders of {@linkplain Session} and {@linkplain StatelessSession}
 * which share state from an underlying {@linkplain Session} or {@linkplain StatelessSession}.
 *
 * @author Steve Ebersole
 */
public interface CommonSharedSessionBuilderOptions extends CommonSessionBuilderOptions {

	/**
	 * Signifies that the connection from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonSharedSessionBuilderOptions connection();

	/**
	 * Signifies the interceptor from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	CommonSharedSessionBuilderOptions interceptor();

	/**
	 * Signifies that the SQL {@linkplain org.hibernate.resource.jdbc.spi.StatementInspector statement inspector}
	 * from the original session should be used.
	 */
	CommonSharedSessionBuilderOptions statementInspector();

	/**
	 * Signifies that no SQL {@linkplain org.hibernate.resource.jdbc.spi.StatementInspector statement inspector}
	 * should be used.
	 */
	CommonSharedSessionBuilderOptions noStatementInspector();

	@Override
	CommonSharedSessionBuilderOptions interceptor(Interceptor interceptor);

	@Override
	CommonSharedSessionBuilderOptions noInterceptor();

	@Override
	CommonSharedSessionBuilderOptions statementInspector(UnaryOperator<String> operator);

	@Override
	CommonSharedSessionBuilderOptions tenantIdentifier(Object tenantIdentifier);
}
