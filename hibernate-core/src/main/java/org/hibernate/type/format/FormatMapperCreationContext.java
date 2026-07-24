/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import org.hibernate.Incubating;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;


/**
 * The creation context for {@link FormatMapper} that is passed as constructor argument to implementations.
 */
@Incubating
public interface FormatMapperCreationContext {
	BootstrapContext getBootstrapContext();

	default ClassLoaderService getClassLoaderService() {
		return getBootstrapContext().getClassLoaderService();
	}

	MappingResolutionOptions getMappingResolutionOptions();
}
