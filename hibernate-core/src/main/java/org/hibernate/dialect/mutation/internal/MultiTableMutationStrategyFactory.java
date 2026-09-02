/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.mutation.internal;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind;
import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;

/// Interprets a Dialect's multi-table mutation fallback profile using
/// Hibernate's runtime mapping model.
///
/// @author Steve Ebersole
@Internal
public final class MultiTableMutationStrategyFactory {
	private MultiTableMutationStrategyFactory() {
	}

	public static SqmMultiTableMutationStrategy createMutationStrategy(
			Dialect dialect,
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext creationContext) {
		final MultiTableMutationStrategyKind strategyKind = requireSupport( dialect ).mutationStrategyKind();
		validate( dialect, rootEntityDescriptor, strategyKind, "update/delete" );
		return switch ( strategyKind ) {
			case CTE -> new CteMutationStrategy( rootEntityDescriptor, creationContext );
			case LOCAL_TEMPORARY_TABLE ->
					new LocalTemporaryTableMutationStrategy( rootEntityDescriptor, creationContext );
			case GLOBAL_TEMPORARY_TABLE ->
					new GlobalTemporaryTableMutationStrategy( rootEntityDescriptor, creationContext );
			case PERSISTENT_TABLE -> new PersistentTableMutationStrategy( rootEntityDescriptor, creationContext );
		};
	}

	public static SqmMultiTableInsertStrategy createInsertStrategy(
			Dialect dialect,
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext creationContext) {
		final MultiTableMutationStrategyKind strategyKind = requireSupport( dialect ).insertStrategyKind();
		validate( dialect, rootEntityDescriptor, strategyKind, "insert" );
		return switch ( strategyKind ) {
			case CTE -> new CteInsertStrategy( rootEntityDescriptor, creationContext );
			case LOCAL_TEMPORARY_TABLE ->
					new LocalTemporaryTableInsertStrategy( rootEntityDescriptor, creationContext );
			case GLOBAL_TEMPORARY_TABLE ->
					new GlobalTemporaryTableInsertStrategy( rootEntityDescriptor, creationContext );
			case PERSISTENT_TABLE -> new PersistentTableInsertStrategy( rootEntityDescriptor, creationContext );
		};
	}

	private static MultiTableMutationSupport requireSupport(Dialect dialect) {
		final MultiTableMutationSupport support = dialect.getMultiTableMutationSupport();
		if ( support == null ) {
			throw new MappingException(
					"Dialect '" + dialect.getClass().getName()
							+ "' returned null from getMultiTableMutationSupport()"
			);
		}
		return support;
	}

	private static void validate(
			Dialect dialect,
			EntityMappingType rootEntityDescriptor,
			MultiTableMutationStrategyKind strategyKind,
			String operation) {
		final String missingPrerequisite = switch ( strategyKind ) {
			case CTE -> dialect.getCteSupport().supports( CteSupport.MutationFeature.NON_QUERY )
					? null
					: "CteSupport.MutationFeature.NON_QUERY";
			case LOCAL_TEMPORARY_TABLE -> dialect.getLocalTemporaryTableStrategy() == null
					? "getLocalTemporaryTableStrategy()"
					: null;
			case GLOBAL_TEMPORARY_TABLE -> dialect.getGlobalTemporaryTableStrategy() == null
					? "getGlobalTemporaryTableStrategy()"
					: null;
			case PERSISTENT_TABLE -> dialect.getPersistentTemporaryTableStrategy() == null
					? "getPersistentTemporaryTableStrategy()"
					: null;
		};
		if ( missingPrerequisite != null ) {
			throw new MappingException(
					"Could not create the " + operation + " multi-table strategy for entity '"
							+ rootEntityDescriptor.getEntityName() + "': " + strategyKind
							+ " requires Dialect prerequisite " + missingPrerequisite
			);
		}
	}
}
