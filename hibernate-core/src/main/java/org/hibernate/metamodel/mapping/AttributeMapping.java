/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.generator.Generator;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.results.graph.DatabaseSnapshotContributor;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlanExposer;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Describes an attribute at the mapping model level.
 *
 * @author Steve Ebersole
 */
public interface AttributeMapping
		extends OwnedValuedModelPart, Fetchable, DatabaseSnapshotContributor, PropertyBasedMapping, MutabilityPlanExposer {

	/**
	 * The name of the mapped attribute, or {@code null} for an unnamed synthetic attribute.
	 */
	@Nullable
	String getAttributeName();

	@Nullable
	@Override
	default String getPartName() {
		return getAttributeName();
	}

	/**
	 * The attribute's position within the container's state array
	 */
	int getStateArrayPosition();

	/**
	 * Access to AttributeMetadata, or {@code null} for a synthetic attribute without metadata.
	 */
	@Nullable
	AttributeMetadata getAttributeMetadata();

	/**
	 * The managed type that declares this attribute, or {@code null} for a synthetic attribute.
	 */
	@Nullable
	ManagedMappingType getDeclaringType();

	/**
	 * The getter/setter access to this attribute, or {@code null} if it has no Java property.
	 */
	@Nullable
	PropertyAccess getPropertyAccess();

	/**
	 * Convenient access to getting the value for this attribute from the declarer
	 */
	@Nullable
	default Object getValue(@Nonnull Object container) {
		return castNonNull( getDeclaringType() ).getValue( container, getStateArrayPosition() );
	}

	/**
	 * Convenient access to setting the value for this attribute on the declarer
	 */
	default void setValue(@Nonnull Object container, @Nullable Object value) {
		castNonNull( getDeclaringType() ).setValue( container, getStateArrayPosition(), value );
	}

	/**
	 * The value generation strategy to use for this attribute.
	 *
	 * @apiNote Only relevant for non-id attributes
	 */
	@Nullable
	Generator getGenerator();

	@Nullable
	@Override
	default EntityMappingType findContainingEntityMapping() {
		final var declaringType = getDeclaringType();
		return declaringType == null ? null : declaringType.findContainingEntityMapping();
	}

	@Nonnull
	@Override
	default MutabilityPlan<?> getExposedMutabilityPlan() {
		return castNonNull( getAttributeMetadata() ).getMutabilityPlan();
	}

	/**
	 * Compare attribute values, treating {@code null} as less than every non-null value.
	 */
	default int compare(@Nullable Object value1, @Nullable Object value2) {
		if ( value1 == value2 ) {
			return 0;
		}
		if ( value1 == null ) {
			return -1;
		}
		if ( value2 == null ) {
			return 1;
		}
		//noinspection unchecked,rawtypes
		return ( (JavaType) getJavaType() ).getComparator().compare( value1, value2 );
	}

	@Nonnull
	@Override //Overrides multiple interfaces!
	default AttributeMapping asAttributeMapping() {
		return this;
	}

	/**
	 * A utility method to avoid casting explicitly to PluralAttributeMapping
	 *
	 * @return PluralAttributeMapping if this is an instance of PluralAttributeMapping otherwise {@code null}
	 */
	@Nullable
	default PluralAttributeMapping asPluralAttributeMapping() {
		return null;
	}

	default boolean isPluralAttributeMapping() {
		return false;
	}

	/**
	 * A utility method to avoid casting explicitly to EmbeddedAttributeMapping
	 *
	 * @return EmbeddedAttributeMapping if this is an instance of EmbeddedAttributeMapping otherwise {@code null}
	 */
	@Nullable
	@org.hibernate.Internal
	default EmbeddedAttributeMapping asEmbeddedAttributeMapping(){
		return null;
	}

	default boolean isEmbeddedAttributeMapping(){
		return false;
	}

}
