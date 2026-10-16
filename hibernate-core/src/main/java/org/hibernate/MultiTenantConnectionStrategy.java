package org.hibernate;

/// Selects how JDBC connections target a tenant. This does not enable or disable
/// discriminator-based multitenancy.
///
/// @see org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_CONNECTION_STRATEGY
/// @since 8.0
/// @author Steve Ebersole
@Incubating(since = "8.0")
public enum MultiTenantConnectionStrategy {

	/// Select a tenant-specific datasource.
	DATABASE,

	/// Select a tenant-specific schema on a JDBC connection.
	SCHEMA
}
