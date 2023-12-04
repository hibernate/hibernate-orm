/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.tenancy;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.models.bind.internal.binders.TenantIdBinder;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class SimpleTenancyTests {
	@SuppressWarnings("JUnitMalformedDeclaration")
	@Test
	@ServiceRegistry
	void testSimpleTenancy(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					var metadataCollector = context.getMetadataCollector();

					assertThat( metadataCollector.getFilterDefinition( TenantIdBinder.FILTER_NAME ) ).isNotNull();

					final PersistentClass entityBinding = metadataCollector.getEntityBinding( ProtectedEntity.class.getName() );
					final Property tenantProperty = entityBinding.getProperty( "tenant" );
					final BasicValue value = (BasicValue) tenantProperty.getValue();
					final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) value.getColumn();

					assertThat( tenantProperty.isUpdateable() ).isFalse();
					assertThat( tenantProperty.isOptional() ).isFalse();

					assertThat( value.getEnumerationStyle() ).isEqualTo( EnumType.ORDINAL );

					assertThat( column.getName() ).isEqualTo( "tenant" );
				},
				scope.getRegistry(),
				ProtectedEntity.class
		);
	}

	@SuppressWarnings("JUnitMalformedDeclaration")
	@Test
	@ServiceRegistry
	void testTenancyWithColumn(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					var metadataCollector = context.getMetadataCollector();

					assertThat( metadataCollector.getFilterDefinition( TenantIdBinder.FILTER_NAME ) ).isNotNull();

					final PersistentClass entityBinding = metadataCollector.getEntityBinding( ProtectedEntityWithColumn.class.getName() );
					final Property tenantProperty = entityBinding.getProperty( "tenant" );
					final BasicValue value = (BasicValue) tenantProperty.getValue();
					final org.hibernate.mapping.Column column = (org.hibernate.mapping.Column) value.getColumn();

					assertThat( tenantProperty.isUpdateable() ).isFalse();
					assertThat( tenantProperty.isOptional() ).isFalse();

					assertThat( value.getEnumerationStyle() ).isEqualTo( EnumType.STRING );

					assertThat( column.getName() ).isEqualTo( "customer" );
				},
				scope.getRegistry(),
				ProtectedEntityWithColumn.class
		);
	}

	enum Tenant { ACME, SPACELY }

	@Entity(name="ProtectedEntity")
	@Table(name="protected_entity")
	public static class ProtectedEntity {
		@Id
		private Integer id;
		private String name;
		@TenantId
		@Enumerated
		private Tenant tenant;
	}

	@Entity(name="ProtectedEntity")
	@Table(name="protected_entity")
	public static class ProtectedEntityWithColumn {
		@Id
		private Integer id;
		private String name;
		@TenantId
		@Enumerated(EnumType.STRING)
		@Column(name="customer")
		private Tenant tenant;
	}
}
