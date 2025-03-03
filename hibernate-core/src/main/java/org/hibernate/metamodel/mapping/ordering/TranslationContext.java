/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.spi.JpaCompliance;

/**
 * Access to information needed while translating a collection's order-by fragment
 *
 * @author Steve Ebersole
 */
public interface TranslationContext {

	SessionFactoryImplementor getFactory();

	default JpaCompliance getJpaCompliance() {
		return getFactory().getSessionFactoryOptions().getJpaCompliance();
	}
}
