/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.ast.internal;

import java.util.Map;

import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.loader.ast.spi.BatchLoaderFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * Initiator for {@link StandardBatchLoaderFactory}
 *
 * @author Steve Ebersole
 */
public class BatchLoaderFactoryInitiator implements StandardServiceInitiator<BatchLoaderFactory> {
	/**
	 * Singleton access
	 */
	public static final BatchLoaderFactoryInitiator INSTANCE = new BatchLoaderFactoryInitiator();

	@Override
	public BatchLoaderFactory initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry) {
		return new StandardBatchLoaderFactory( configurationValues, registry );
	}

	@Override
	public Class<BatchLoaderFactory> getServiceInitiated() {
		return BatchLoaderFactory.class;
	}
}
