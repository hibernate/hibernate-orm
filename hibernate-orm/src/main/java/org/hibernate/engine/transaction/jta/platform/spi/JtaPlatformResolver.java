/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.transaction.jta.platform.spi;

import java.util.Map;

import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Service for defining how to resolve or determine the {@link JtaPlatform} to use in configurations where the user
 * did not explicitly specify one.
 *
 * @author Steve Ebersole
 */
public interface JtaPlatformResolver extends Service {
	public JtaPlatform resolveJtaPlatform(Map configurationValues, ServiceRegistryImplementor registry);
}
