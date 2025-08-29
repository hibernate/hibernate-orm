/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import org.hibernate.engine.creation.CommonSharedBuilder;

import java.util.function.UnaryOperator;

/**
 * Allows for creation of a {@linkplain StatelessSession stateless session} sharing the
 * underpinnings of another {@linkplain Session stateful} or {@linkplain StatelessSession stateless}
 * session.
 *
 * @see Session#statelessWithOptions()
 * @see StatelessSession#statelessWithOptions()
 *
 * @since 7.2
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SharedStatelessSessionBuilder extends CommonSharedBuilder {
	/**
	 * Open the stateless session.
	 */
	StatelessSession open();

	@Override
	SharedStatelessSessionBuilder connection();

	@Override
	SharedStatelessSessionBuilder interceptor();

	@Override
	SharedStatelessSessionBuilder interceptor(Interceptor interceptor);

	@Override
	SharedStatelessSessionBuilder noInterceptor();

	SharedStatelessSessionBuilder statementInspector(UnaryOperator<String> operator);

	@Override
	SharedStatelessSessionBuilder statementInspector();

	@Override
	SharedStatelessSessionBuilder noStatementInspector();

	@Override
	SharedStatelessSessionBuilder tenantIdentifier(Object tenantIdentifier);

	@Override
	SharedStatelessSessionBuilder readOnly(boolean readOnly);

	@Override
	SharedStatelessSessionBuilder initialCacheMode(CacheMode cacheMode);
}
