/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;

/**
 * @author Steve Ebersole
 */
public interface DomainModelProducer {
	MetadataImplementor produceModel(StandardServiceRegistry serviceRegistry);
}
