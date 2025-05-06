/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Context ("parameter object") used in resolving a {@link NamedResultSetMappingMementoImpl}
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ResultSetMappingResolutionContext {
	SessionFactoryImplementor getSessionFactory();

	default MappingMetamodel getMappingMetamodel() {
		return getSessionFactory().getMappingMetamodel();
	}

	default TypeConfiguration getTypeConfiguration() {
		return getSessionFactory().getTypeConfiguration();
	}

	default NamedObjectRepository getNamedObjectRepository() {
		return getSessionFactory().getQueryEngine().getNamedObjectRepository();
	}
}
