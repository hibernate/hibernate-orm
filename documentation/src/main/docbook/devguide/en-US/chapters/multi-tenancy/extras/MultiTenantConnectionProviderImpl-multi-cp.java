/**
 * Simplisitc implementation for illustration purposes supporting 2 hard coded providers (pools) and leveraging
 * the support class {@link org.hibernate.service.jdbc.connections.spi.AbstractMultiTenantConnectionProvider}
 */
public class MultiTenantConnectionProviderImpl extends AbstractMultiTenantConnectionProvider {
	private final ConnectionProvider acmeProvider = ConnectionProviderUtils.buildConnectionProvider( "acme" );
	private final ConnectionProvider jbossProvider = ConnectionProviderUtils.buildConnectionProvider( "jboss" );

	@Override
	protected ConnectionProvider getAnyConnectionProvider() {
		return acmeProvider;
	}

	@Override
	protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
		if ( "acme".equals( tenantIdentifier ) ) {
			return acmeProvider;
		}
		else if ( "jboss".equals( tenantIdentifier ) ) {
			return jbossProvider;
		}
		throw new HibernateException( "Unknown tenant identifier" );
	}
}