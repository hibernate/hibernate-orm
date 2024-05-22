/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.util.TimeZone;

/**
 * Specialized {@link SessionBuilder} with access to stuff from another session.
 *
 * @author Steve Ebersole
 *
 * @see Session#sessionWithOptions()
 */
public interface SharedSessionBuilder extends SessionBuilder {

	/**
	 * Signifies that the connection from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder connection();

	/**
	 * Signifies the interceptor from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder interceptor();

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @deprecated use {@link #connectionHandlingMode} instead.
	 */
	@Deprecated(since = "6.0")
	SharedSessionBuilder connectionReleaseMode();

	/**
	 * Signifies that the connection release mode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder connectionHandlingMode();

	/**
	 * Signifies that the autoJoinTransaction flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder autoJoinTransactions();

	/**
	 * Signifies that the FlushMode from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder flushMode();

	/**
	 * Signifies that the autoClose flag from the original session should be used to create the new session.
	 *
	 * @return {@code this}, for method chaining
	 */
	SharedSessionBuilder autoClose();

	@Override
	SharedSessionBuilder statementInspector(StatementInspector statementInspector);

	@Override
	SharedSessionBuilder connectionHandlingMode(PhysicalConnectionHandlingMode mode);

	@Override
	SharedSessionBuilder autoClear(boolean autoClear);

	@Override
	SharedSessionBuilder flushMode(FlushMode flushMode);

	@Override @Deprecated(forRemoval = true)
	SharedSessionBuilder tenantIdentifier(String tenantIdentifier);

	@Override
	SharedSessionBuilder tenantIdentifier(Object tenantIdentifier);

	@Override
	SharedSessionBuilder eventListeners(SessionEventListener... listeners);

	@Override
	SharedSessionBuilder clearEventListeners();

	@Override
	SharedSessionBuilder jdbcTimeZone(TimeZone timeZone);

	@Override
	SharedSessionBuilder interceptor(Interceptor interceptor);

	@Override
	SharedSessionBuilder noInterceptor();

	@Override
	SharedSessionBuilder connection(Connection connection);

	@Override
	SharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions);

	@Override
	SharedSessionBuilder autoClose(boolean autoClose);
}
