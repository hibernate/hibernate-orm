/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.creation.internal.options;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.CacheMode;
import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Interceptor;
import org.hibernate.SessionCreationOption;
import org.hibernate.StatementObserver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.EmptyInterceptor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.sql.Connection;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;

/// Mutable collector for options common to creation of both stateful
/// and stateless sessions.
///
/// Instances are populated by session builders and by the Jakarta
/// Persistence type-safe creation option support, then consumed through
/// the [org.hibernate.engine.creation.internal.SessionCreationOptions]
/// contract when constructing a session.
///
/// The constructor initializes the collector with the defaults supplied
/// by the [SessionFactoryImplementor], but the factory is not retained.
/// Behaviors which require the factory at consumption time, such as
/// interceptor resolution, take it explicitly.
///
/// @since 8.0
/// @author Steve Ebersole
public class CommonOptions {
	protected StatementInspector statementInspector;
	protected StatementObserver statementObserver;
	protected Interceptor interceptor;
	protected boolean allowInterceptor = true;
	protected boolean allowSessionInterceptorCreation = true;
	protected Connection connection;
	protected PhysicalConnectionHandlingMode connectionHandlingMode;
	protected Object tenantIdentifier;
	protected boolean readOnly;
	protected Integer jdbcBatchSize;
	protected CacheMode cacheMode;
	protected TimeZone jdbcTimeZone;
	protected Object temporalIdentifier;
	protected List<SessionCreationOption.EnabledFilter> enabledFilterOptions;
	private int defaultBatchFetchSize;
	private boolean subselectFetchEnabled;

	public CommonOptions(SessionFactoryImplementor sessionFactory) {
		final var options = sessionFactory.getSessionFactoryOptions();
		statementInspector = options.getStatementInspector();
		cacheMode = options.getInitialSessionCacheMode();
		connectionHandlingMode = options.getPhysicalConnectionHandlingMode();
		jdbcTimeZone = options.getJdbcTimeZone();
		tenantIdentifier = sessionFactory.resolveTenantIdentifier();
		defaultBatchFetchSize = options.getDefaultBatchFetchSize();
		subselectFetchEnabled = options.isSubselectFetchEnabled();
	}

	@Nullable
	public StatementInspector getStatementInspector() {
		return statementInspector;
	}

	@Nullable
	public StatementObserver getStatementObserver() {
		return statementObserver;
	}

	@Nullable
	public Connection getConnection() {
		return connection;
	}

	@Nonnull
	public PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode() {
		return connectionHandlingMode;
	}

	@Nullable
	public Object getTenantIdentifierValue() {
		return tenantIdentifier;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	@Nullable
	public Integer getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	@Nonnull
	public CacheMode getInitialCacheMode() {
		return cacheMode;
	}

	@Nullable
	public TimeZone getJdbcTimeZone() {
		return jdbcTimeZone;
	}

	@Nullable
	public Object getTemporalIdentifier() {
		return temporalIdentifier;
	}

	@Nonnull
	public List<SessionCreationOption.EnabledFilter> getEnabledFilterOptions() {
		return enabledFilterOptions == null ? emptyList() : enabledFilterOptions;
	}

	public void connection(Connection connection) {
		this.connection = connection;
	}

	public void connectionHandling(ConnectionAcquisitionMode acquisitionMode, ConnectionReleaseMode releaseMode) {
		this.connectionHandlingMode = PhysicalConnectionHandlingMode.interpret( acquisitionMode, releaseMode );
	}

	public void connectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
		this.connectionHandlingMode = connectionHandlingMode;
	}

	public void interceptor(Interceptor interceptor) {
		if ( interceptor == null ) {
			noInterceptor();
		}
		else {
			this.interceptor = interceptor;
			this.allowInterceptor = true;
		}
	}

	public void useInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
		this.allowInterceptor = true;
	}

	public void noInterceptor() {
		this.interceptor = null;
		this.allowInterceptor = false;
	}

	public void noSessionInterceptorCreation() {
		this.allowSessionInterceptorCreation = false;
	}

	public void tenantIdentifier(Object tenantIdentifier) {
		this.tenantIdentifier = tenantIdentifier;
	}

	public void jdbcBatchSize(int batchSize) {
		this.jdbcBatchSize = batchSize;
	}

	public void readOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void initialCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	public void cacheStoreMode(CacheStoreMode cacheStoreMode) {
		initialCacheMode( CacheMode.interpretStoreMode( cacheMode, cacheStoreMode ) );
	}

	public void cacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		initialCacheMode( CacheMode.interpretRetrieveMode( cacheMode, cacheRetrieveMode ) );
	}

	public void statementInspector(UnaryOperator<String> operator) {
		if ( operator == null ) {
			noStatementInspector();
		}
		else {
			this.statementInspector = operator::apply;
		}
	}

	public void statementInspector(StatementInspector statementInspector) {
		this.statementInspector = statementInspector;
	}

	public void noStatementInspector() {
		this.statementInspector = null;
	}

	public void statementObserver(StatementObserver statementObserver) {
		this.statementObserver = statementObserver;
	}

	public void jdbcTimeZone(TimeZone timeZone) {
		jdbcTimeZone = timeZone;
	}

	public void asOf(Instant instant) {
		this.temporalIdentifier = instant;
	}

	public void atChangeset(Object changesetId) {
		this.temporalIdentifier = changesetId;
	}

	public void defaultBatchFetchSize(int defaultBatchFetchSize) {
		this.defaultBatchFetchSize = defaultBatchFetchSize;
	}

	public void subselectFetchEnabled(boolean subselectFetchEnabled) {
		this.subselectFetchEnabled = subselectFetchEnabled;
	}

	public boolean isSubselectFetchEnabled() {
		return subselectFetchEnabled;
	}

	public int getDefaultBatchFetchSize() {
		return defaultBatchFetchSize;
	}

	public void enableFilter(SessionCreationOption.EnabledFilter enabledFilter) {
		if ( enabledFilterOptions == null ) {
			enabledFilterOptions = new ArrayList<>();
		}
		enabledFilterOptions.add( enabledFilter );
	}

	/// Resolve the interceptor to use for the session being created.
	///
	/// This is intentionally not a simple getter. Resolution must account for
	/// an explicitly disabled interceptor, an explicitly supplied interceptor,
	/// the factory-scoped interceptor, and the factory's session-scoped
	/// interceptor supplier.
	@Nullable
	public Interceptor resolveInterceptor(@Nonnull SessionFactoryImplementor sessionFactory) {
		if ( !allowInterceptor ) {
			return null;
		}

		if ( interceptor != null && interceptor != EmptyInterceptor.INSTANCE ) {
			return interceptor;
		}

		final var options = sessionFactory.getSessionFactoryOptions();

		final var optionsInterceptor = options.getInterceptor();
		if ( optionsInterceptor != null && optionsInterceptor != EmptyInterceptor.INSTANCE ) {
			return optionsInterceptor;
		}

		if ( allowSessionInterceptorCreation ) {
			final var statelessInterceptorImplementorSupplier =
					options.getStatelessInterceptorImplementorSupplier();
			if ( statelessInterceptorImplementorSupplier != null ) {
				return statelessInterceptorImplementorSupplier.get();
			}
		}

		return null;
	}
}
