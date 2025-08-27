/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.service.JavaServiceLoadable;

/**
 * Contract for contributing to the initialization of {@link MetadataBuilder}.
 * Called immediately after any configuration settings have been applied from
 * {@link org.hibernate.engine.config.spi.ConfigurationService}. Any values
 * specified here override those. Any values set here can still be overridden
 * explicitly by the user via the exposed methods of {@link MetadataBuilder}.
 *
 * @author Steve Ebersole
 *
 * @since 5.0
 */
@JavaServiceLoadable
public interface MetadataBuilderInitializer {
	void contribute(MetadataBuilder metadataBuilder, StandardServiceRegistry serviceRegistry);
}
