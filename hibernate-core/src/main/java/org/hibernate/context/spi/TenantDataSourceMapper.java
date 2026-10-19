package org.hibernate.context.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;

/**
 * Supplies the JNDI name of a {@link javax.sql.DataSource} for a given tenant identifier.
 * An implementation is selected using
 * {@value org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_DATASOURCE_MAPPER},
 * or obtained from the configured bean container.
 * <p>
 * Configuring a mapper automatically enables
 * {@link org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl}
 * unless a custom multi-tenant connection provider is supplied.
 *
 * @param <T> The type of the tenant id
 *
 * @see org.hibernate.cfg.MultiTenancySettings#MULTI_TENANT_DATASOURCE_MAPPER
 * @see org.hibernate.cfg.Configuration#setTenantDataSourceMapper
 * @see org.hibernate.boot.SessionFactoryBuilder#applyTenantDataSourceMapper
 * @see org.hibernate.engine.jdbc.connections.spi.DataSourceBasedMultiTenantConnectionProviderImpl#DataSourceBasedMultiTenantConnectionProviderImpl(TenantDataSourceMapper)
 *
 * @since 8.0
 */
@Incubating(since = "8.0")
public interface TenantDataSourceMapper<T> {
	/**
	 * The full JNDI name of the data source belonging to the given tenant.
	 * Data sources are cached, so this method need not be called for every connection.
	 *
	 * @param tenantIdentifier The tenant identifier
	 * @return The JNDI name of the data source belonging to that tenant
	 */
	@Nonnull String dataSourceName(@Nonnull T tenantIdentifier);
}
