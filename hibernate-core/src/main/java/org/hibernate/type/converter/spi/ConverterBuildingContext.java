/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.converter.spi;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Access to information needed while building an AttributeConverter instance
 *
 * @author Steve Ebersole
 */
public interface ConverterBuildingContext {
	TypeConfiguration getTypeConfiguration();

	ServiceRegistry getServiceRegistry();

	Object getBeanManagerReference();
}