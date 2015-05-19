/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.service.spi;

/**
 * Optional {@link org.hibernate.service.Service} contract for services which can be managed in JMX
 *
 * @author Steve Ebersole
 */
public interface Manageable {
	/**
	 * Get the domain name to be used in registering the management bean.  May be {@code null} to indicate Hibernate's
	 * default domain ({@code org.hibernate.core}) should be used.
	 *
	 * @return The management domain.
	 */
	public String getManagementDomain();

	/**
	 * Allows the service to specify a special 'serviceType' portion of the object name.  {@code null} indicates
	 * we should use the default scheme, which is to use the name of the service impl class for this purpose.
	 *
	 * @return The custom 'serviceType' name.
	 */
	public String getManagementServiceType();

	/**
	 * The the management bean (MBean) for this service.
	 *
	 * @return The management bean.
	 */
	public Object getManagementBean();
}
