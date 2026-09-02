/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.mutation.internal.MultiTableMutationStrategyFactory;
import org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind;
import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.mutation.internal.SqmMultiTableMutationStrategyProviderStandard;
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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.CTE;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.GLOBAL_TEMPORARY_TABLE;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.LOCAL_TEMPORARY_TABLE;
import static org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind.PERSISTENT_TABLE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies internal interpretation of the multi-table mutation fallback profile.
///
/// @author Steve Ebersole
public class MultiTableMutationStrategyFactoryTests {
	private static final String ENTITY_NAME = "ExampleRoot";

	@Test
	void createsBothOperationStrategiesForEveryKind() {
		for ( MultiTableMutationStrategyKind kind : MultiTableMutationStrategyKind.values() ) {
			final Dialect dialect = supportingDialect( MultiTableMutationSupport.forBoth( kind ), kind );
			assertThat( createMutationStrategy( kind, dialect ) ).isInstanceOf( mutationType( kind ) );
			assertThat( createInsertStrategy( kind, dialect ) ).isInstanceOf( insertType( kind ) );
		}
	}

	@Test
	void asymmetricProfileSelectsEachComponentIndependently() {
		final Dialect dialect = supportingDialect(
				new MultiTableMutationSupport( CTE, LOCAL_TEMPORARY_TABLE ),
				CTE,
				LOCAL_TEMPORARY_TABLE
		);
		assertThat( createMutationStrategy( CTE, dialect ) ).isInstanceOf( CteMutationStrategy.class );
		assertThat( createInsertStrategy( LOCAL_TEMPORARY_TABLE, dialect ) )
				.isInstanceOf( LocalTemporaryTableInsertStrategy.class );
	}

	@Test
	void everyMissingPrerequisiteFailsBeforeConstructionWithContext() {
		assertMissingPrerequisite( CTE, "CteSupport.MutationFeature.NON_QUERY" );
		assertMissingPrerequisite( LOCAL_TEMPORARY_TABLE, "getLocalTemporaryTableStrategy()" );
		assertMissingPrerequisite( GLOBAL_TEMPORARY_TABLE, "getGlobalTemporaryTableStrategy()" );
		assertMissingPrerequisite( PERSISTENT_TABLE, "getPersistentTemporaryTableStrategy()" );
	}

	@Test
	void nullDialectProfileFailsAtTheFactoryBoundary() {
		final Dialect dialect = mock( Dialect.class );
		assertThatExceptionOfType( MappingException.class )
				.isThrownBy( () -> MultiTableMutationStrategyFactory.createMutationStrategy(
						dialect,
						rootEntity(),
						creationContext()
				) )
				.withMessageContaining( dialect.getClass().getName() )
				.withMessageContaining( "getMultiTableMutationSupport()" );
	}

	@Test
	void globalCustomStrategiesTakePrecedenceWithoutConsultingTheDialect() {
		final var provider = new SqmMultiTableMutationStrategyProviderStandard();
		final EntityMappingType rootEntity = rootEntity();
		final RuntimeModelCreationContext creationContext = creationContext();
		final SessionFactoryOptions options = creationContext.getSessionFactoryOptions();
		final SqmMultiTableMutationStrategy mutationStrategy = mock( SqmMultiTableMutationStrategy.class );
		final SqmMultiTableInsertStrategy insertStrategy = mock( SqmMultiTableInsertStrategy.class );
		when( options.getCustomSqmMultiTableMutationStrategy() ).thenReturn( mutationStrategy );
		when( options.getCustomSqmMultiTableInsertStrategy() ).thenReturn( insertStrategy );

		assertThat( provider.createMutationStrategy( rootEntity, creationContext ) ).isSameAs( mutationStrategy );
		assertThat( provider.createInsertStrategy( rootEntity, creationContext ) ).isSameAs( insertStrategy );
		verify( creationContext, never() ).getDialect();
	}

	@Test
	void entityCustomStrategiesTakePrecedenceWithoutConsultingTheDialect() {
		final var provider = new SqmMultiTableMutationStrategyProviderStandard();
		final EntityMappingType rootEntity = rootEntity();
		final RuntimeModelCreationContext creationContext = creationContext();
		final SessionFactoryOptions options = creationContext.getSessionFactoryOptions();
		final SqmMultiTableMutationStrategy mutationStrategy = mock( SqmMultiTableMutationStrategy.class );
		final SqmMultiTableInsertStrategy insertStrategy = mock( SqmMultiTableInsertStrategy.class );
		when( options.resolveCustomSqmMultiTableMutationStrategy( rootEntity, creationContext ) )
				.thenReturn( mutationStrategy );
		when( options.resolveCustomSqmMultiTableInsertStrategy( rootEntity, creationContext ) )
				.thenReturn( insertStrategy );

		assertThat( provider.createMutationStrategy( rootEntity, creationContext ) ).isSameAs( mutationStrategy );
		assertThat( provider.createInsertStrategy( rootEntity, creationContext ) ).isSameAs( insertStrategy );
		verify( creationContext, never() ).getDialect();
	}

	private static void assertMissingPrerequisite(
			MultiTableMutationStrategyKind kind,
			String prerequisite) {
		final Dialect dialect = mock( Dialect.class );
		when( dialect.getMultiTableMutationSupport() ).thenReturn( MultiTableMutationSupport.forBoth( kind ) );
		if ( kind == CTE ) {
			when( dialect.getCteSupport() ).thenReturn( CteSupport.NONE );
		}

		assertThatExceptionOfType( MappingException.class )
				.isThrownBy( () -> MultiTableMutationStrategyFactory.createMutationStrategy(
						dialect,
						rootEntity(),
						creationContext()
				) )
				.withMessageContaining( ENTITY_NAME )
				.withMessageContaining( "update/delete" )
				.withMessageContaining( kind.name() )
				.withMessageContaining( prerequisite );

		assertThatExceptionOfType( MappingException.class )
				.isThrownBy( () -> MultiTableMutationStrategyFactory.createInsertStrategy(
						dialect,
						rootEntity(),
						creationContext()
				) )
				.withMessageContaining( ENTITY_NAME )
				.withMessageContaining( "insert" )
				.withMessageContaining( kind.name() )
				.withMessageContaining( prerequisite );
	}

	private static Dialect supportingDialect(
			MultiTableMutationSupport support,
			MultiTableMutationStrategyKind... kinds) {
		final Dialect dialect = mock( Dialect.class );
		when( dialect.getMultiTableMutationSupport() ).thenReturn( support );
		for ( MultiTableMutationStrategyKind kind : kinds ) {
			switch ( kind ) {
				case CTE -> when( dialect.getCteSupport() ).thenReturn( CteSupport.builder()
						.placement( CteSupport.Placement.TOP_LEVEL )
						.mutationFeatures( CteSupport.MutationFeature.NON_QUERY )
						.build() );
				case LOCAL_TEMPORARY_TABLE -> when( dialect.getLocalTemporaryTableStrategy() )
						.thenReturn( mock( TemporaryTableStrategy.class ) );
				case GLOBAL_TEMPORARY_TABLE -> when( dialect.getGlobalTemporaryTableStrategy() )
						.thenReturn( mock( TemporaryTableStrategy.class ) );
				case PERSISTENT_TABLE -> when( dialect.getPersistentTemporaryTableStrategy() )
						.thenReturn( mock( TemporaryTableStrategy.class ) );
			}
		}
		return dialect;
	}

	private static SqmMultiTableMutationStrategy createMutationStrategy(
			MultiTableMutationStrategyKind kind,
			Dialect dialect) {
		return switch ( kind ) {
			case CTE -> {
				try ( var ignored = mockConstruction( CteMutationStrategy.class ) ) {
					yield MultiTableMutationStrategyFactory.createMutationStrategy(
							dialect,
							rootEntity(),
							creationContext()
					);
				}
			}
			case LOCAL_TEMPORARY_TABLE -> {
				try ( var ignored = mockConstruction( LocalTemporaryTableMutationStrategy.class ) ) {
					yield MultiTableMutationStrategyFactory.createMutationStrategy(
							dialect,
							rootEntity(),
							creationContext()
					);
				}
			}
			case GLOBAL_TEMPORARY_TABLE -> {
				try ( var ignored = mockConstruction( GlobalTemporaryTableMutationStrategy.class ) ) {
					yield MultiTableMutationStrategyFactory.createMutationStrategy(
							dialect,
							rootEntity(),
							creationContext()
					);
				}
			}
			case PERSISTENT_TABLE -> {
				try ( var ignored = mockConstruction( PersistentTableMutationStrategy.class ) ) {
					yield MultiTableMutationStrategyFactory.createMutationStrategy(
							dialect,
							rootEntity(),
							creationContext()
					);
				}
			}
		};
	}

	private static SqmMultiTableInsertStrategy createInsertStrategy(
			MultiTableMutationStrategyKind kind,
			Dialect dialect) {
		return switch ( kind ) {
			case CTE -> {
				try ( var ignored = mockConstruction( CteInsertStrategy.class ) ) {
					yield MultiTableMutationStrategyFactory.createInsertStrategy(
							dialect,
							rootEntity(),
							creationContext()
					);
				}
			}
			case LOCAL_TEMPORARY_TABLE -> {
				try ( var ignored = mockConstruction( LocalTemporaryTableInsertStrategy.class ) ) {
					yield MultiTableMutationStrategyFactory.createInsertStrategy(
							dialect,
							rootEntity(),
							creationContext()
					);
				}
			}
			case GLOBAL_TEMPORARY_TABLE -> {
				try ( var ignored = mockConstruction( GlobalTemporaryTableInsertStrategy.class ) ) {
					yield MultiTableMutationStrategyFactory.createInsertStrategy(
							dialect,
							rootEntity(),
							creationContext()
					);
				}
			}
			case PERSISTENT_TABLE -> {
				try ( var ignored = mockConstruction( PersistentTableInsertStrategy.class ) ) {
					yield MultiTableMutationStrategyFactory.createInsertStrategy(
							dialect,
							rootEntity(),
							creationContext()
					);
				}
			}
		};
	}

	private static Class<? extends SqmMultiTableMutationStrategy> mutationType(
			MultiTableMutationStrategyKind kind) {
		return switch ( kind ) {
			case CTE -> CteMutationStrategy.class;
			case LOCAL_TEMPORARY_TABLE -> LocalTemporaryTableMutationStrategy.class;
			case GLOBAL_TEMPORARY_TABLE -> GlobalTemporaryTableMutationStrategy.class;
			case PERSISTENT_TABLE -> PersistentTableMutationStrategy.class;
		};
	}

	private static Class<? extends SqmMultiTableInsertStrategy> insertType(
			MultiTableMutationStrategyKind kind) {
		return switch ( kind ) {
			case CTE -> CteInsertStrategy.class;
			case LOCAL_TEMPORARY_TABLE -> LocalTemporaryTableInsertStrategy.class;
			case GLOBAL_TEMPORARY_TABLE -> GlobalTemporaryTableInsertStrategy.class;
			case PERSISTENT_TABLE -> PersistentTableInsertStrategy.class;
		};
	}

	private static EntityMappingType rootEntity() {
		final EntityMappingType rootEntity = mock( EntityMappingType.class );
		when( rootEntity.getEntityName() ).thenReturn( ENTITY_NAME );
		return rootEntity;
	}

	private static RuntimeModelCreationContext creationContext() {
		final RuntimeModelCreationContext creationContext = mock( RuntimeModelCreationContext.class );
		when( creationContext.getSessionFactoryOptions() ).thenReturn( mock( SessionFactoryOptions.class ) );
		return creationContext;
	}

	private static MappingModelCreationProcess creationProcess(RuntimeModelCreationContext creationContext) {
		final MappingModelCreationProcess creationProcess = mock( MappingModelCreationProcess.class );
		when( creationProcess.getCreationContext() ).thenReturn( creationContext );
		return creationProcess;
	}
}
