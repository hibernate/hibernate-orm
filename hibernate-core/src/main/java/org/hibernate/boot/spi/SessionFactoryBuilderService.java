/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.service.Service;

/**
 * Allows bootstrapping Hibernate ORM using a custom {@link SessionFactoryBuilderImplementor}.
 */
public interface SessionFactoryBuilderService extends Service {

	SessionFactoryBuilderImplementor createSessionFactoryBuilder(MetadataImpl metadata, BootstrapContext bootstrapContext);

}
