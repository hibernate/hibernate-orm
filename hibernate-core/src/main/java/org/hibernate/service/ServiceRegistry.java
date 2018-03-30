/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service;

/**
 * A registry of {@link Service services}.
 *
 * @author Steve Ebersole
 */
public interface ServiceRegistry extends AutoCloseable {
	/**
	 * Retrieve this registry's parent registry.
	 * 
	 * @return The parent registry.  May be null.
	 */
	ServiceRegistry getParentServiceRegistry();

	/**
	 * Retrieve a service by role.  If service is not found, but a {@link org.hibernate.service.spi.ServiceInitiator} is
	 * registered for this service role, the service will be initialized and returned.
	 * <p/>
	 * NOTE: We cannot return {@code <R extends Service<T>>} here because the service might come from the parent...
	 * 
	 * @param serviceRole The service role
	 * @param <R> The service role type
	 *
	 * @return The requested service or null if the service was not found.
	 *
	 * @throws UnknownServiceException Indicates the service was not known.
	 */
	<R extends Service> R getService(Class<R> serviceRole);

	/**
	 * Retrieve a service by role.  If service is not found, but a {@link org.hibernate.service.spi.ServiceInitiator} is
	 * registered for this service role, the service will be initialized and returned.
	 * <p/>
	 * NOTE: We cannot return {@code <R extends Service<T>>} here because the service might come from the parent...
	 *
	 * @param serviceRole The service role
	 * @param <R> The service role type
	 *
	 * @return The requested service .
	 *
	 * @throws UnknownServiceException Indicates the service was not known.
	 * @throws NullServiceException Indicates the service was null.
	 */
	default <R extends Service> R requireService(Class<R> serviceRole) {
		final R service = getService( serviceRole );
		if ( service == null ) {
			throw new NullServiceException( serviceRole );
		}
		return service;
	}

	@Override
	void close();
}
