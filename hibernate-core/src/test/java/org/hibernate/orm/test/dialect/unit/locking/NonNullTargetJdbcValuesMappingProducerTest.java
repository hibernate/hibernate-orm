package org.hibernate.orm.test.dialect.unit.locking;

import java.util.HashSet;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.internal.NonNullTargetJdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.internal.NonNullTargetJdbcValuesMappingProducer.TargetedMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingResolution;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies that target metadata preserves custom mapping and assembler
/// resolution while lock modes remain specific to each execution.
///
/// @author Steve Ebersole
class NonNullTargetJdbcValuesMappingProducerTest {
	@Test
	void preservesProviderMappingAndAssemblerResolution() {
		final var provider = mock( JdbcValuesMappingProducer.class );
		final var original = mock( JdbcValuesMapping.class );
		final var metadata = mock( JdbcValuesMetadata.class );
		final var influencers = mock( LoadQueryInfluencers.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var resolution = mock( JdbcValuesMappingResolution.class );
		final List<SqlSelection> selections = List.of( mock( SqlSelection.class ) );
		final List<DomainResult<?>> results = List.of( mock( DomainResult.class ) );
		final int[] indexes = { 0, -1 };
		when( provider.resolve( metadata, influencers, factory ) ).thenReturn( original );
		when( original.getSqlSelections() ).thenReturn( selections );
		when( original.getDomainResults() ).thenReturn( results );
		when( original.getRowSize() ).thenReturn( 2 );
		when( original.getValueIndexesToCacheIndexes() ).thenReturn( indexes );
		when( original.getRowToCacheSize() ).thenReturn( 1 );
		when( original.resolveAssemblers( factory ) ).thenReturn( resolution );
		when( original.determineDefaultLockMode( "child", LockMode.READ ) ).thenReturn( LockMode.NONE );

		final var producer = new NonNullTargetJdbcValuesMappingProducer( provider, "root" );
		final var mapping = producer.resolve( metadata, influencers, factory );
		assertThat( mapping.getSqlSelections() ).isSameAs( selections );
		assertThat( mapping.getDomainResults() ).isSameAs( results );
		assertThat( mapping.getRowSize() ).isEqualTo( 2 );
		assertThat( mapping.getValueIndexesToCacheIndexes() ).isSameAs( indexes );
		assertThat( mapping.getRowToCacheSize() ).isEqualTo( 1 );
		assertThat( mapping.resolveAssemblers( factory ) ).isSameAs( resolution );
		assertThat( mapping.determineDefaultLockMode( "child", LockMode.READ ) ).isEqualTo( LockMode.NONE );
		final var tables = new HashSet<String>();
		producer.addAffectedTableNames( tables, factory );
		verify( provider ).addAffectedTableNames( tables, factory );
		// Distinct target metadata must not share the provider's cache identity.
		assertThat( producer.cacheKeyInstance() ).isSameAs( producer ).isNotSameAs( provider.cacheKeyInstance() );
	}

	@Test
	void doesNotCaptureExecutionLockModesOrInfluencers() {
		final var provider = mock( JdbcValuesMappingProducer.class );
		final var metadata = mock( JdbcValuesMetadata.class );
		final var factory = mock( SessionFactoryImplementor.class );
		final var firstInfluencers = mock( LoadQueryInfluencers.class );
		final var secondInfluencers = mock( LoadQueryInfluencers.class );
		when( provider.resolve( metadata, firstInfluencers, factory ) ).thenReturn( mock( JdbcValuesMapping.class ) );
		when( provider.resolve( metadata, secondInfluencers, factory ) ).thenReturn( mock( JdbcValuesMapping.class ) );
		final var producer = new NonNullTargetJdbcValuesMappingProducer( provider, "root" );
		final var first = (TargetedMapping) producer.resolve( metadata, firstInfluencers, factory );
		final var second = (TargetedMapping) producer.resolve( metadata, secondInfluencers, factory );
		assertThat( second ).isNotSameAs( first );
		for ( var mode : List.of( LockMode.PESSIMISTIC_READ, LockMode.PESSIMISTIC_WRITE, LockMode.PESSIMISTIC_FORCE_INCREMENT ) ) {
			assertThat( first.determineTargetLockMode( "root", mode ) ).isEqualTo( mode );
			assertThat( second.determineTargetLockMode( "root", mode ) ).isEqualTo( mode );
			assertThat( first.determineTargetLockMode( "child", mode ) ).isEqualTo( LockMode.NONE );
			assertThat( second.determineTargetLockMode( null, mode ) ).isEqualTo( LockMode.NONE );
		}
		verify( provider ).resolve( metadata, firstInfluencers, factory );
		verify( provider ).resolve( metadata, secondInfluencers, factory );
	}
}
