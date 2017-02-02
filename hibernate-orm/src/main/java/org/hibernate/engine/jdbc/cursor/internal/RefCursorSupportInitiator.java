/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.cursor.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Service initiator for RefCursorSupport service
 *
 * @author Steve Ebersole
 */
public class RefCursorSupportInitiator implements StandardServiceInitiator<RefCursorSupport> {
	/**
	 * Singleton access
	 */
	public static final RefCursorSupportInitiator INSTANCE = new RefCursorSupportInitiator();

	@Override
	public RefCursorSupport initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
		return new StandardRefCursorSupport();
	}

	@Override
	public Class<RefCursorSupport> getServiceInitiated() {
		return RefCursorSupport.class;
	}
}
