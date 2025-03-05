/*
 * SPDX-License-Identifier: Apache-2.0
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
