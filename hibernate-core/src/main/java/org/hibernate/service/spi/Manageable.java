/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
