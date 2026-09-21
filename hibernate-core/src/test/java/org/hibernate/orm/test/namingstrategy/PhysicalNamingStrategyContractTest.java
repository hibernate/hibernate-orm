/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Exercises physical-strategy inputs, quoting finalization, and real binding paths.
///
/// @author Steve Ebersole
@BaseUnitTest
class PhysicalNamingStrategyContractTest {
	@Test
	void strategyCannotRemoveMappingQuoting() {
		final var environment = environment();
		final var result = PhysicalNamingStrategyHelper.resolve(
				new LogicalName( "Mixed", true, true ), environment,
				(name, context) -> context.getPhysicalNameFactory().create( "Renamed", false ), "column", false );
		assertThat( result.getText() ).isEqualTo( "Renamed" );
		assertThat( result.isQuoted() ).isTrue();
		final var factory = environment.getIdentifierHelper().getPhysicalNameFactory();
		assertThat( result ).isEqualTo( factory.create( "Renamed", true ) );
		assertThat( result ).isNotEqualTo( factory.create( "Renamed", false ) );
	}

	@Test
	void strategyCanAddQuotingAndKeywordsAreCheckedAfterTransformation() {
		final var environment = environment();
		final var logical = new LogicalName( "ordinary", false, false );
		assertThat( PhysicalNamingStrategyHelper.resolve( logical, environment,
				(name, context) -> context.getPhysicalNameFactory().create( "Mixed", true ), "column", false ).isQuoted() )
				.isTrue();
		assertThat( PhysicalNamingStrategyHelper.resolve( logical, environment,
				(name, context) -> context.getPhysicalNameFactory().create( "select", false ), "column", false ).isQuoted() )
				.isTrue();
	}

	@Test
	void qualificationMayBeAbsentOrSuppliedButRequiredNamesMustExist() {
		final var environment = environment();
		assertThat( PhysicalNamingStrategyHelper.resolve( null, environment,
				(name, context) -> null, "schema", true ) ).isNull();
		assertThat( PhysicalNamingStrategyHelper.resolve( null, environment,
				(name, context) -> context.getPhysicalNameFactory().create( "tenant", false ), "schema", true ).getText() )
				.isEqualTo( "tenant" );
		assertThatThrownBy( () -> PhysicalNamingStrategyHelper.resolve( new LogicalName( "required", false, true ),
				environment, (name, context) -> null, "column", false ) )
				.isInstanceOf( MappingException.class ).hasMessageContaining( "column" ).hasMessageContaining( "required" );
	}

	@Test
	void globalQuotingFollowsSnakeCaseAndPreservesSourceQuoting() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( GLOBALLY_QUOTED_IDENTIFIERS, true ).build()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming(
					registry, new MappingSources().addManagedClass( CustomerRecord.class ).addManagedClass( ImplicitRecord.class ),
					ImplicitNamingStrategyJpaCompliantImpl.INSTANCE, new PhysicalNamingStrategySnakeCaseImpl() );
			final var entity = metadata.getEntityBinding( CustomerRecord.class.getName() );
			assertThat( entity.getTable().getName() ).isEqualTo( "customer_record" );
			assertThat( entity.getTable().isQuoted() ).isTrue();
			final var implicitTable = metadata.getEntityBinding( ImplicitRecord.class.getName() ).getTable();
			assertThat( implicitTable.getName() ).isEqualTo( "implicit_record" );
			assertThat( implicitTable.isQuoted() ).isTrue();
			final var ordinary = entity.getProperty( "orderCount" ).getColumns().get( 0 );
			assertThat( ordinary.getName() ).isEqualTo( "order_count" );
			assertThat( ordinary.isQuoted() ).isTrue();
			final var quoted = entity.getProperty( "code" ).getColumns().get( 0 );
			assertThat( quoted.getName() ).isEqualTo( "MixedCode" );
			assertThat( quoted.isQuoted() ).isTrue();
		}
	}

	@Test
	void explicitTableOriginReachesStrategy() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var strategy = new PhysicalNamingStrategyStandardImpl() {
				@Override
				public PhysicalName toPhysicalTableName(LogicalName name, PhysicalNamingContext context) {
					assertThat( name.isExplicit() ).isTrue();
					return super.toPhysicalTableName( name, context );
				}
			};
			MetadataBuildingTestHelper.buildMetadataWithNaming(
					registry, new MappingSources().addManagedClass( CustomerRecord.class ),
					ImplicitNamingStrategyJpaCompliantImpl.INSTANCE, strategy );
		}
	}

	private static JdbcEnvironment environment() {
		final var environment = mock( JdbcEnvironment.class );
		final var builder = IdentifierHelperBuilder.from( environment );
		builder.applyReservedWords( "select" );
		when( environment.getIdentifierHelper() ).thenReturn( builder.build() );
		return environment;
	}

	@Entity(name = "CustomerRecord")
	@Table(name = "CustomerRecord")
	static class CustomerRecord {
		@Id long id;
		int orderCount;
		@Column(name = "`MixedCode`") String code;
	}
	@Entity(name = "ImplicitRecord")
	static class ImplicitRecord {
		@Id long id;
	}

}
