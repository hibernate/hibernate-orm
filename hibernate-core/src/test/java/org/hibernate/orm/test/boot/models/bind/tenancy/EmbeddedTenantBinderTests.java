/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.tenancy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.TenantId;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.AttributeBindingContext;
import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.Component;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;

/// Exercises internal embedded tenant processing alongside public custom binders.
///
/// @author Steve Ebersole
@BaseUnitTest
class EmbeddedTenantBinderTests {

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void embeddedTenantRetainsFilterCustomBinderAndRowSecurity(boolean rowSecurityEnabled) {
		final var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( DIALECT, SimpleTenancyTests.RlsDialect.class )
				.applySetting( MULTI_TENANT_RLS_ENABLED, rowSecurityEnabled )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, Document.class );
			final var entity = metadata.getEntityBinding( Document.class.getName() );
			final var tenant = ( (Component) entity.getProperty( "ownership" ).getValue() ).getProperty( "tenant" );
			assertThat( tenant.isUpdatable() ).isFalse();
			assertThat( tenant.isOptional() ).isFalse();
			assertThat( entity.getFilters() ).hasSize( 1 );
			assertThat( entity.getFilters().get( 0 ).getName() ).isEqualTo( TenantIdBinder.FILTER_NAME );
			assertThat( entity.getFilters().get( 0 ).getCondition() ).isEqualTo( "tenant_id = :tenantId" );
			assertThat( metadata.getFilterDefinition( TenantIdBinder.FILTER_NAME ).getParameterJdbcMapping(
					TenantIdBinder.PARAMETER_NAME ).getJavaTypeDescriptor().getJavaTypeClass() ).isEqualTo( String.class );
			final var column = (org.hibernate.mapping.Column) tenant.getSelectables().get( 0 );
			assertThat( column.getComment() ).isEqualTo( "custom-bound" );
			final var sqlContext = SqlStringGenerationContextImpl.fromExplicit(
					metadata.getDatabase().getJdbcEnvironment(), metadata.getDatabase(), null, null );
			final var ddl = metadata.getDatabase().getAuxiliaryDatabaseObjects().stream()
					.flatMap( object -> java.util.Arrays.stream( object.sqlCreateStrings( sqlContext ) ) )
					.toList();
			if ( rowSecurityEnabled ) {
				assertThat( ddl ).containsExactly( "rls:embedded_tenant_document:tenant_id:SESSION" );
			}
			else {
				assertThat( ddl ).isEmpty();
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void conflictingRootAndEmbeddedTenantTypesAreRejected() {
		final var registry = ServiceRegistryUtil.serviceRegistryBuilder().build();
		try {
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadata(
					registry, Document.class, NumericTenantDocument.class ) )
					.hasStackTraceContaining( "all @TenantId fields must have the same type" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Entity(name = "EmbeddedTenantDocument")
	@Table(name = "embedded_tenant_document")
	static class Document {
		@Id
		long id;
		@Embedded
		Ownership ownership;
	}

	@Embeddable
	static class Ownership {
		@TenantId
		@CustomTenant
		@Column(name = "tenant_id")
		String tenant;
	}

	@Entity(name = "NumericTenantDocument")
	static class NumericTenantDocument {
		@Id
		long id;
		@TenantId
		Long tenant;
	}

	@Target(FIELD)
	@Retention(RUNTIME)
	@AttributeBinderType(binder = CustomTenantBinder.class)
	public @interface CustomTenant {
	}

	public static class CustomTenantBinder implements AttributeBinder<CustomTenant> {
		@Override
		public void bind(CustomTenant annotation, AttributeBindingContext context) {
			assertThat( context.getAttribute().usage().member().getName() ).isEqualTo( "tenant" );
			final var column = (org.hibernate.mapping.Column) context.getProperty().getSelectables().get( 0 );
			assertThat( column.getComment() ).isNull();
			column.setComment( "custom-bound" );
		}
	}
}
