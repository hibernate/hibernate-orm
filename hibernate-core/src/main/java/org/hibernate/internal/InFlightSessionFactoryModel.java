/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.util.Objects;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.metamodel.internal.RuntimeMetamodelsImpl;
import org.hibernate.metamodel.model.domain.internal.MappingMetamodelImpl;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sql.internal.FetchProfileRegistry;
import org.hibernate.query.sql.spi.SqlTranslationEngine;

import static org.hibernate.internal.FetchProfileHelper.addFetchProfiles;

/// In-flight runtime model and query infrastructure assembled during default
/// `SessionFactoryImpl` construction.
///
/// This is intentionally a two-phase product. Runtime model initialization
/// currently calls back through the partially constructed `SessionFactoryImpl`,
/// so the factory must assign the query and SQL infrastructure fields before
/// the mapping model can be finished.
///
/// @since 9.0
/// @author Steve Ebersole
record InFlightSessionFactoryModel(
		RuntimeMetamodelsImpl runtimeMetamodels,
		QueryEngine queryEngine,
		SqlTranslationEngine sqlTranslationEngine,
		MappingMetamodelImpl mappingMetamodel,
		FetchProfileRegistry fetchProfileRegistry) {

	InFlightSessionFactoryModel {
		Objects.requireNonNull( runtimeMetamodels );
		Objects.requireNonNull( queryEngine );
		Objects.requireNonNull( sqlTranslationEngine );
		Objects.requireNonNull( mappingMetamodel );
		Objects.requireNonNull( fetchProfileRegistry );
	}

	void finishModelInitialization(
			MetadataImplementor bootMetamodel,
			RuntimeModelCreationContext modelCreationContext) {
		mappingMetamodel.finishInitialization( modelCreationContext );
		runtimeMetamodels.setJpaMetamodel( mappingMetamodel.getJpaMetamodel() );

		// This needs to happen after the mapping metamodel is completely built,
		// since fetch profiles resolve against the finished persisters.
		addFetchProfiles( bootMetamodel, runtimeMetamodels, fetchProfileRegistry );
	}

	JpaMetamodelImplementor jpaMetamodel() {
		return mappingMetamodel.getJpaMetamodel();
	}
}
