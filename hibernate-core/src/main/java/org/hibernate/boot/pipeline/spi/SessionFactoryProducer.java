/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.pipeline.spi;

import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.JavaServiceLoadable;

/// Service-loadable hook for producing a custom [SessionFactoryImplementor]
/// from finalized bootstrap products.
///
/// Hibernate ORM provides the default producer when no service-loaded producer is
/// discovered.  A bootstrap may have at most one discovered producer unless a
/// producer selector setting names the producer to use.
///
/// @since 9.0
/// @author Steve Ebersole
@JavaServiceLoadable
public interface SessionFactoryProducer {

	/// The unique name used to select this producer when multiple producers are
	/// available.
	///
	/// @see PersistenceSettings#SESSION_FACTORY_PRODUCER
	String getProducerName();

	/// Produce the SessionFactory from the given request.
	SessionFactoryImplementor buildSessionFactory(SessionFactoryConstructionRequest request);
}
