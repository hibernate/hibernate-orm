/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import jakarta.annotation.Nonnull;

import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;

import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.ToOne;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Logical referenced names retain their target-column identities under physical naming.
///
/// @author Steve Ebersole
@BaseUnitTest
class CompositeReferenceNamingTest {
	@Test
	void logicalReferencesResolveReversedCompositeJoinColumns() {
		verifyComposite( new PrefixNaming(), "p_" );
	}

	@Test
	void logicalReferencesResolveWithIdentityPhysicalNaming() {
		verifyComposite( PhysicalNamingStrategyStandardImpl.INSTANCE, "" );
	}

	@Test
	void physicalNamesDoNotSubstituteForLogicalReferences() {
		try ( var registry = ServiceRegistryUtil.serviceRegistry() ) {
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming(
					registry,
					new MappingSources().addManagedClass( Target.class ).addManagedClass( PhysicalReference.class ),
					ImplicitNamingStrategyJpaCompliantImpl.INSTANCE,
					new PrefixNaming()
			) ).isInstanceOf( org.hibernate.MappingException.class )
					.hasMessageContaining( "Could not resolve non-primary-key association target columns" );
		}
	}

	private void verifyComposite(PhysicalNamingStrategyStandardImpl strategy, String prefix) {
		try ( var registry = ServiceRegistryUtil.serviceRegistry() ) {
			final var metadata = (MetadataImplementor) MetadataBuildingTestHelper.buildMetadataWithNaming(
					registry,
					new MappingSources().addManagedClass( Target.class ).addManagedClass( LogicalReference.class ),
					ImplicitNamingStrategyJpaCompliantImpl.INSTANCE,
					strategy
			);
			metadata.orderColumns( false );
			metadata.validate();
			final var association = (ToOne) metadata.getEntityBinding( LogicalReference.class.getName() )
					.getProperty( "target" ).getValue();
			assertThat( association.isReferenceToPrimaryKey() ).isTrue();
			assertThat( association.getForeignKeyColumnMappings().mappings() )
					.extracting( pair -> pair.column().getName() + "->" + pair.referencedColumn().getName() )
					.containsExactlyInAnyOrder( prefix + "target_country->" + prefix + "country",
							prefix + "target_number->" + prefix + "number" );
		}
	}

	public static class PrefixNaming extends PhysicalNamingStrategyStandardImpl {
		@Override
		@Nonnull
		public PhysicalName toPhysicalColumnName(@Nonnull LogicalName name, @Nonnull PhysicalNamingContext environment) {
			return name == null ? null : environment.getPhysicalNameFactory().create( "p_" + name.getText(), name.isQuoted() );
		}
	}

	@Embeddable
	public static class TargetId {
		@Column(name = "country")
		String country;
		@Column(name = "number")
		Integer number;
	}

	@Entity(name = "NamingTarget")
	public static class Target {
		@EmbeddedId
		TargetId id;
	}

	@Entity(name = "PhysicalReference")
	public static class PhysicalReference {
		@Id
		Long id;
		@ManyToOne
		@JoinColumns({
				@JoinColumn(name = "target_number", referencedColumnName = "p_number"),
				@JoinColumn(name = "target_country", referencedColumnName = "p_country")
		})
		Target target;
	}

	@Entity(name = "LogicalReference")
	public static class LogicalReference {
		@Id
		Long id;
		@ManyToOne
		@JoinColumns({
				@JoinColumn(name = "target_number", referencedColumnName = "number"),
				@JoinColumn(name = "target_country", referencedColumnName = "country")
		})
		Target target;
	}
}
