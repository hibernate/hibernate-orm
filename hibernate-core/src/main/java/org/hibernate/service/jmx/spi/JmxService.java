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
package org.hibernate.service.jmx.spi;

import javax.management.ObjectName;

import org.hibernate.service.Service;
import org.hibernate.service.spi.Manageable;

/**
 * Service providing simplified access to JMX related features needed by Hibernate.
 *
 * @author Steve Ebersole
 */
public interface JmxService extends Service {
	/**
	 * Handles registration of a manageable service.
	 *
	 * @param service The manageable service
	 * @param serviceRole The service's role.
	 */
	public void registerService(Manageable service, Class<? extends Service> serviceRole);

	/**
	 * Registers the given {@code mBean} under the given {@code objectName}
	 *
	 * @param objectName The name under which to register the MBean
	 * @param mBean The MBean to register
	 */
	public void registerMBean(ObjectName objectName, Object mBean);
}
