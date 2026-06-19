/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.extension;

import jakarta.annotation.Nullable;

import org.hibernate.boot.mapping.internal.materialize.CollationMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.NaturalIdMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.TenantIdMappingMaterializer;
import org.hibernate.boot.mapping.internal.model.AttributeUsageBinding;
import org.hibernate.boot.mapping.internal.model.CollationContribution;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.NaturalIdContribution;
import org.hibernate.boot.mapping.internal.model.TenantIdBinding;
import org.hibernate.boot.mapping.internal.view.CollationContributionView;
import org.hibernate.boot.mapping.internal.view.NaturalIdContributionView;
import org.hibernate.boot.mapping.internal.view.TenantIdBindingView;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.type.BasicType;

/// Standard internal adapter from capability-oriented contribution calls to the
/// current binding model and compatibility materializers.
///
/// Public SPI candidates should not expose this class.  It is deliberately the
/// place where today's materialization details are hidden behind the
/// capability-oriented [AttributeBindingTarget] contract.
///
/// @since 9.0
/// @author Steve Ebersole
public class StandardAttributeBindingTarget implements AttributeBindingTarget {
	private final IdentifiableTypeMetadata ownerType;
	private final AttributeUsageBinding usage;
	private final @Nullable Property property;
	private final @Nullable RootClass rootClass;
	private final BindingContributionContext context;

	private @Nullable Property contributedProperty;

	private final AttributeOptions options = new StandardAttributeOptions();
	private final SelectableTarget selectables = new StandardSelectableTarget();
	private final EntityTarget entity = new StandardEntityTarget();

	public static StandardAttributeBindingTarget forProperty(
			IdentifiableTypeMetadata ownerType,
			AttributeUsageBinding usage,
			Property property,
			BindingContributionContext context) {
		return new StandardAttributeBindingTarget( ownerType, usage, property, null, context );
	}

	public static StandardAttributeBindingTarget forEntityAttribute(
			EntityTypeMetadata ownerType,
			AttributeUsageBinding usage,
			RootClass rootClass,
			BindingContributionContext context) {
		return new StandardAttributeBindingTarget( ownerType, usage, null, rootClass, context );
	}

	private StandardAttributeBindingTarget(
			IdentifiableTypeMetadata ownerType,
			AttributeUsageBinding usage,
			@Nullable Property property,
			@Nullable RootClass rootClass,
			BindingContributionContext context) {
		this.ownerType = ownerType;
		this.usage = usage;
		this.property = property;
		this.rootClass = rootClass;
		this.context = context;
	}

	@Override
	public AttributeUsageBinding usage() {
		return usage;
	}

	@Override
	public AttributeOptions options() {
		return options;
	}

	@Override
	public SelectableTarget selectables() {
		return selectables;
	}

	@Override
	public EntityTarget entity() {
		return entity;
	}

	public @Nullable Property contributedProperty() {
		return contributedProperty;
	}

	private Property requireProperty(String contributionName) {
		if ( property == null ) {
			throw new IllegalStateException( contributionName + " requires a materialized attribute property" );
		}
		return property;
	}

	private class StandardAttributeOptions implements AttributeOptions {
		@Override
		public void naturalId(boolean mutable) {
			final var contribution = new NaturalIdContribution(
					ownerType,
					usage.attributeName(),
					usage.member(),
					mutable
			);
			context.bindingState().getBootBindingModel().addNaturalIdContribution( contribution );
			new NaturalIdMappingMaterializer().materializeNaturalId(
					new NaturalIdContributionView( contribution ),
					requireProperty( "@NaturalId" )
			);
		}
	}

	private class StandardSelectableTarget implements SelectableTarget {
		@Override
		public void collation(String collation) {
			final var contribution = new CollationContribution(
					ownerType,
					collationAttributePath(),
					usage.member(),
					collation
			);
			context.bindingState().getBootBindingModel().addCollationContribution( contribution );
			new CollationMappingMaterializer().materializeCollation(
					new CollationContributionView( contribution ),
					requireProperty( "@Collate" )
			);
		}

		private String collationAttributePath() {
			return usage.usageContainer() instanceof ManagedTypeBinding
					? usage.attributePath()
					: usage.sourceRole();
		}
	}

	private class StandardEntityTarget implements EntityTarget {
		@Override
		public void tenantId(BasicType<?> tenantIdType) {
			if ( !( ownerType instanceof EntityTypeMetadata entityType ) ) {
				throw new IllegalStateException( "@TenantId requires an entity usage container" );
			}
			if ( rootClass == null ) {
				throw new IllegalStateException( "@TenantId requires an entity materialization target" );
			}
			final var tenantIdBinding = new TenantIdBinding(
					entityType,
					usage.attributeName(),
					usage.member(),
					usage.basicValueIntent(),
					tenantIdType
			);
			context.bindingState().getBootBindingModel().addTenantIdBinding( entityType, tenantIdBinding );
			contributedProperty = new TenantIdMappingMaterializer().materializeTenantId(
					new TenantIdBindingView( tenantIdBinding ),
					rootClass,
					context.bindingOptions(),
					context.bindingState(),
					context.bindingContext()
			);
		}
	}
}
