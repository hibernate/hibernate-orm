/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.BasicColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.model.naming.spi.TenantColumnNamingInput;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.mapping.Component;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;

/// Verifies tenant-specific naming, explicit bypass, inherited and embedded paths,
/// physical finalization, logical references, and runtime tenant isolation.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitTenantColumnNamingTest {
	@ParameterizedTest
	@MethodSource("org.hibernate.orm.test.namingstrategy.ImplicitNamingStrategyMatrixTest#cases")
	void defaultsPreserveTerminalNames(ImplicitNamingStrategyMatrixTest.Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Document.class, EmbeddedDocument.class ),
					test.strategy().implementation, new ImplicitNamingStrategyMatrixTest.PhysicalStrategy( test.prefix() ) );
			assertThat( metadata.getEntityBinding( Document.class.getName() ).getProperty( "tenant" ).getColumns().get( 0 ).getName() )
					.isEqualTo( test.physical( "tenant" ) );
			final var details = (Component) metadata.getEntityBinding( EmbeddedDocument.class.getName() ).getProperty( "details" ).getValue();
			final var ownership = (Component) details.getProperty( "ownership" ).getValue();
			assertThat( ownership.getProperty( "tenant" ).getColumns().get( 0 ).getName() ).isEqualTo( test.physical( "tenant" ) );
		}
	}

	@Test
	void callbacksAndLogicalReferencesUseTheSameResult() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder().applySetting( MULTI_TENANT_RLS_ENABLED, false ).build()) {
			final var strategy = new TenantStrategy();
			final var physical = new PrefixStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Reference.class, Document.class, EmbeddedDocument.class, ExplicitDocument.class ),
					strategy, physical );
			assertThat( strategy.inputs ).extracting( TenantColumnNamingInput::attributePath )
					.containsExactlyInAnyOrder( "tenant", "details.ownership.tenant" );
			assertThat( strategy.inputs ).filteredOn( input -> input.attributePath().equals( "tenant" ) ).singleElement()
					.satisfies( input -> {
						assertThat( input.entity().getClassName() ).isEqualTo( Document.class.getName() );
						assertThat( input.entity().getJpaEntityName() ).isEqualTo( "TenantNamingDocument" );
					} );
			final var document = metadata.getEntityBinding( Document.class.getName() );
			final var column = document.getProperty( "tenant" ).getColumns().get( 0 );
			assertThat( column.getName() ).isEqualTo( "p_scope_tenant" );
			assertThat( column.isQuoted() ).isTrue();
			assertThat( document.getFilters().get( 0 ).getCondition() )
					.isEqualTo( column.getQuotedName( metadata.getDatabase().getDialect() ) + " = :tenantId" );
			final var embedded = metadata.getEntityBinding( EmbeddedDocument.class.getName() );
			assertThat( embedded.getFilters().get( 0 ).getCondition() ).contains( "p_scope_details_ownership_tenant" );
			final var explicit = metadata.getEntityBinding( ExplicitDocument.class.getName() ).getProperty( "tenant" ).getColumns().get( 0 );
			assertThat( explicit.getName() ).isEqualTo( "p_chosen_tenant" );
			assertThat( explicit.isQuoted() ).isTrue();
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "scope_tenant" ) ).singleElement()
					.satisfies( name -> assertThat( name.isExplicit() ).isFalse() );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "scope_details_ownership_tenant" ) ).hasSize( 1 );
			assertThat( physical.inputs ).filteredOn( name -> name.getText().equals( "chosen_tenant" ) ).singleElement()
					.satisfies( name -> assertThat( name.isExplicit() ).isTrue() );
			// Resolving this non-PK reference requires the generated logical-name correspondence.
			assertThat( metadata.getEntityBinding( Reference.class.getName() ).getProperty( "document" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "p_document_tenant" );
		}
	}

	@ParameterizedTest @ValueSource(booleans = { true, false })
	void invalidResultsAreRejected(boolean returnNull) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithImplicitNaming( registry,
					new MappingSources().addManagedClass( Document.class ), new StandardImplicitNamingStrategy() {
						@Override
						@Nonnull
						@SuppressWarnings("DataFlowIssue") // Deliberately invalid strategy result.
						public LogicalName determineTenantColumnName(@Nonnull TenantColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
							return returnNull ? null : new LogicalName( "invalid", false, true );
						}
					} ) ).hasMessageContaining( "non-null implicit name for tenant column" );
		}
	}

	@Test
	void filteringUsesCustomPhysicalTenantColumns() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" )
				.applySetting( MULTI_TENANT_RLS_ENABLED, false ).build()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Document.class, EmbeddedDocument.class ), new TenantStrategy(), new PrefixStrategy() );
			try (var factory = SessionFactoryPipeline.build( metadata, new SessionFactoryOptionsCollector() )) {
				for ( String tenant : List.of( "first", "second" ) ) {
					try (var session = factory.withOptions().tenantIdentifier( tenant ).openSession()) {
						final var tx = session.beginTransaction();
						final var document = new Document();
						document.id = tenant.equals( "first" ) ? 1 : 2;
						session.persist( document );
						final var embedded = new EmbeddedDocument();
						embedded.id = document.id;
						session.persist( embedded );
						tx.commit();
					}
				}
				for ( String tenant : List.of( "first", "second" ) ) {
					try (var session = factory.withOptions().tenantIdentifier( tenant ).openSession()) {
						assertThat( session.createQuery( "from TenantNamingDocument", Document.class ).getResultList() )
								.singleElement().satisfies( document -> assertThat( document.tenant ).isEqualTo( tenant ) );
						assertThat( session.createQuery( "from EmbeddedTenantNamingDocument", EmbeddedDocument.class ).getResultList() )
								.singleElement().satisfies( document -> assertThat( document.details.ownership.tenant ).isEqualTo( tenant ) );
					}
				}
			}
		}
	}

	static class TenantStrategy extends StandardImplicitNamingStrategy {
		final List<TenantColumnNamingInput> inputs = new ArrayList<>();
		@Override
		@Nonnull
		public LogicalName determineTenantColumnName(@Nonnull TenantColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
			inputs.add( input );
			return context.implicitName( "scope_" + input.attributePath().replace( '.', '_' ), true );
		}
		@Override
		@Nonnull
		public LogicalName determineBasicColumnName(@Nonnull BasicColumnNamingInput input, @Nonnull ImplicitNamingContext context) {
			assertThat( input.attributePath() ).doesNotEndWith( "tenant" );
			return super.determineBasicColumnName( input, context );
		}
	}
	static class PrefixStrategy extends PhysicalNamingStrategyStandardImpl {
		final List<LogicalName> inputs = new ArrayList<>();
		@Override
		@Nonnull
		public PhysicalName toPhysicalColumnName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext context) {
			inputs.add( name );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), false );
		}
	}
	@MappedSuperclass
	static class TenantBase { @TenantId @Column(unique = true) String tenant; }
	@Entity(name = "TenantNamingDocument")
	static class Document extends TenantBase { @Id long id; }
	@Entity(name = "EmbeddedTenantNamingDocument")
	static class EmbeddedDocument { @Id long id; @Embedded Details details = new Details(); }
	@Embeddable
	static class Details { @Embedded Ownership ownership = new Ownership(); }
	@Embeddable
	static class Ownership { @TenantId String tenant; }
	@Entity(name = "ExplicitTenantNamingDocument")
	static class ExplicitDocument { @Id long id; @TenantId @Column(name = "`chosen_tenant`") String tenant; }
	@Entity(name = "TenantNamingReference")
	static class Reference {
		@Id long id;
		@ManyToOne @JoinColumn(name = "document_tenant", referencedColumnName = "`scope_tenant`") Document document;
	}
}
