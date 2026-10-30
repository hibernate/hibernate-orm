package org.hibernate.orm.test.sql.exec.spi;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.dialect.sql.ast.internal.H2SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.tree.spi.select.DynamicInstantiationNature;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.select.QueryGroup;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.lock.LockingHelper;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.entity.EntityResult;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchImpl;
import org.hibernate.sql.results.graph.instantiation.internal.ArgumentDomainResult;
import org.hibernate.sql.results.graph.instantiation.internal.DynamicInstantiationResultImpl;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.graph.internal.LockingResultCoverage;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.testing.orm.junit.Jira;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

/// Structural registration coverage independent of JDBC and session state.
///
/// @author Steve Ebersole
@Jira("https://hibernate.atlassian.net/browse/HHH-21026")
class FollowOnResultCoverageTest {
	@Test
	void nonScalarFlagDoesNotEstablishIdentityCoverage() {
		final var opaque = mock( DomainResult.class );
		when( opaque.containsAnyNonScalarResults() ).thenReturn( true );
		assertThat( LockingResultCoverage.from( opaque ).entityPaths() ).isEmpty();
	}

	@Test
	void delayedEntityFetchDoesNotEstablishIdentityCoverage() {
		final var fetch = mock( EntityDelayedFetchImpl.class );
		when( fetch.getTiming() ).thenReturn( FetchTiming.DELAYED );
		when( fetch.getNavigablePath() ).thenReturn( new NavigablePath( "item.target" ) );
		assertThat( LockingResultCoverage.from( fetch ).entityPaths() ).isEmpty();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void nestedArgumentsRetainEntityCoverageWithoutExposingMutableSets() {
		final var path = new NavigablePath( "item" );
		final var nested = new DynamicInstantiationResultImpl( "nested", DynamicInstantiationNature.LIST,
				mock( JavaType.class ), List.of( new ArgumentDomainResult<>( entity( path ) ) ) );
		final var outer = new DynamicInstantiationResultImpl( "outer", DynamicInstantiationNature.LIST,
				mock( JavaType.class ), List.of( new ArgumentDomainResult<>( nested ) ) );
		final var coverage = LockingResultCoverage.from( outer );
		assertThat( coverage.entityPaths() ).containsExactly( path );
		assertThat( LockingResultCoverage.EMPTY.entityPaths() ).isEmpty();
		assertThatThrownBy( () -> coverage.entityPaths().clear() ).isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	void uncoveredTargetsAreDeduplicatedAndSorted() {
		final var spec = new QuerySpec( true );
		final var a = new NavigablePath( "Item", "a" );
		final var z = new NavigablePath( "Item", "z" );
		spec.applyRootPathForLocking( z );
		spec.applyRootPathForLocking( a );
		spec.applyRootPathForLocking( a );
		final var strategy = mock( LockingClauseStrategy.class );
		when( strategy.getPathsToLock() ).thenReturn( List.of( z, a ) );
		assertThatThrownBy( () -> LockingHelper.validateFollowOnLockingResults( new SelectStatement( spec ), spec, strategy ) )
				.isInstanceOf( IllegalQueryOperationException.class )
				.hasMessageContaining( "uncovered entity lock targets: [Item(a), Item(z)]" );
	}

	@Test
	void querySpecCoverageUpdatesReplaceImmutableSnapshots() {
		final var a = new NavigablePath( "Item", "a" );
		final var b = new NavigablePath( "Item", "b" );
		final var first = new QuerySpec( true );
		final var second = new QuerySpec( true );
		assertThat( first.getLockingResultCoverage() ).isNull();
		first.applyDomainResultForLocking( entity( a ) );
		final var original = first.getLockingResultCoverage();
		first.applyDomainResultForLocking( entity( b ) );
		second.applyDomainResultForLocking( entity( b ) );
		assertThat( original.entityPaths() ).containsExactly( a );
		assertThat( first.getLockingResultCoverage().entityPaths() ).containsExactlyInAnyOrder( a, b );
		assertThat( second.getLockingResultCoverage().entityPaths() ).containsExactly( b );
		assertThat( LockingResultCoverage.EMPTY.entityPaths() ).isEmpty();
	}

	@Test
	void laterBranchMustSupplyItsOwnCoverage() {
		final var a = new NavigablePath( "Item", "a" );
		final var b = new NavigablePath( "Item", "b" );
		final var first = branch( a );
		first.applyDomainResultForLocking( entity( a ) );
		final var second = branch( b );
		second.applyDomainResultForLocking( mock( DomainResult.class ) );
		final var statement = new SelectStatement( new QueryGroup( true, SetOperator.UNION_ALL, List.of( first, second ) ),
				List.of( entity( a ) ) );
		final var strategy = mock( LockingClauseStrategy.class );
		assertThatThrownBy( () -> LockingHelper.validateFollowOnLockingResults( statement, first, strategy ) )
				.hasMessageContaining( "branch 2: Item(b)" );
		second.applyDomainResultForLocking( entity( b ) );
		assertThatCode( () -> LockingHelper.validateFollowOnLockingResults( statement, first, strategy ) ).doesNotThrowAnyException();
	}

	@Test
	void loaderCreatedResultAndPaginationWrappersRetainCoverage() {
		final var path = new NavigablePath( "Item" );
		final var spec = branch( path );
		final var strategy = mock( LockingClauseStrategy.class );
		when( strategy.getPathsToLock() ).thenReturn( List.of( path ) );
		assertThatCode( () -> LockingHelper.validateFollowOnLockingResults(
				new SelectStatement( spec, List.of( entity( path ) ) ), spec, strategy ) ).doesNotThrowAnyException();
		spec.applyDomainResultForLocking( entity( path ) );
		final var wrapper = spec.asSubQuery();
		assertThat( wrapper.getLockingResultCoverage() ).isSameAs( spec.getLockingResultCoverage() );
		assertThat( wrapper.getRootPathsForLocking() ).containsExactly( path );
		assertThatCode( () -> LockingHelper.validateFollowOnLockingResults(
				new SelectStatement( new QuerySpec( true ) ), spec, strategy ) ).doesNotThrowAnyException();
	}

	@Test
	void actualSubqueryFollowOnLockingRemainsUnsupported() {
		final var factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		when( factory.getJdbcServices().getDialect() ).thenReturn( new H2Dialect() );
		final var options = new QueryOptionsImpl();
		options.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE ).setFollowOnStrategy( Locking.FollowOn.FORCE );
		final var statement = new SelectStatement( new QuerySpec( false ) );
		assertThatThrownBy( () -> new H2SqlAstTranslator<>( new SqlAstTranslationRequest.Select( factory, statement ) )
				.translate( null, options ) ).isInstanceOf( UnsupportedOperationException.class )
				.hasMessage( "Follow-on locking for subqueries is not supported" );
	}

	private static QuerySpec branch(NavigablePath path) {
		final var spec = new QuerySpec( true );
		spec.applyRootPathForLocking( path );
		return spec;
	}

	private static EntityResult<?> entity(NavigablePath path) {
		final var entity = mock( EntityResult.class );
		when( entity.getNavigablePath() ).thenReturn( path );
		when( entity.getFetches() ).thenReturn( ImmutableFetchList.EMPTY );
		return entity;
	}
}
