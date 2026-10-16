package org.hibernate.boot.internal;

import jakarta.annotation.Nullable;
import org.hibernate.MultiTenantConnectionStrategy;
import org.hibernate.boot.spi.AbstractDelegatingSessionFactoryOptions;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantSchemaMapper;

/// Freezes tenant connection options for one session factory so that subsequent
/// builder changes cannot change its connection strategy or tenant resolution.
/// The remaining factory options retain their existing delegation behavior.
///
/// @author Steve Ebersole
final class TenantConnectionOptions extends AbstractDelegatingSessionFactoryOptions {
	private final boolean multiTenancyEnabled;
	private final @Nullable MultiTenantConnectionStrategy connectionStrategy;
	private final @Nullable TenantSchemaMapper<Object> schemaMapper;
	private final @Nullable TenantCredentialsMapper<Object> credentialsMapper;
	private final @Nullable CurrentTenantIdentifierResolver<Object> identifierResolver;

	TenantConnectionOptions(SessionFactoryOptions options) {
		super( options );
		multiTenancyEnabled = options.isMultiTenancyEnabled();
		connectionStrategy = options.getMultiTenantConnectionStrategy();
		schemaMapper = options.getTenantSchemaMapper();
		credentialsMapper = options.getTenantCredentialsMapper();
		identifierResolver = options.getCurrentTenantIdentifierResolver();
	}

	@Override
	public boolean isMultiTenancyEnabled() {
		return multiTenancyEnabled;
	}

	@Override
	@Nullable
	public MultiTenantConnectionStrategy getMultiTenantConnectionStrategy() {
		return connectionStrategy;
	}

	@Override
	@Nullable
	public TenantSchemaMapper<Object> getTenantSchemaMapper() {
		return schemaMapper;
	}

	@Override
	@Nullable
	public TenantCredentialsMapper<Object> getTenantCredentialsMapper() {
		return credentialsMapper;
	}

	@Override
	@Nullable
	public CurrentTenantIdentifierResolver<Object> getCurrentTenantIdentifierResolver() {
		return identifierResolver;
	}
}
