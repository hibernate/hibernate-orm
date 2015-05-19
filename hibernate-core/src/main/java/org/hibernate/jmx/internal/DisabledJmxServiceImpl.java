/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jmx.internal;
import javax.management.ObjectName;

import org.hibernate.jmx.spi.JmxService;
import org.hibernate.service.spi.Manageable;

/**
 * A no-op version of the {@link JmxService}
 *
 * @author Steve Ebersole
 */
public class DisabledJmxServiceImpl  implements JmxService {
	public static final DisabledJmxServiceImpl INSTANCE = new DisabledJmxServiceImpl();

	@Override
	public void registerService(Manageable service, Class serviceRole) {
	}

	@Override
	public void registerMBean(ObjectName objectName, Object mBean) {
	}
}
