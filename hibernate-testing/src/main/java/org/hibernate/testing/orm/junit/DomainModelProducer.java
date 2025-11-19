/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.MetadataImplementor;

/// Alternative to [@DomainModel][DomainModel] for defining the
/// [domain model][MetadataImplementor] to use for testing.
/// Generally used in conjunction with [DomainModelFunctionalTesting].
///
/// @author Steve Ebersole
public interface DomainModelProducer {
	/// Produce the domain model
	MetadataImplementor produceModel(StandardServiceRegistry serviceRegistry);
}
