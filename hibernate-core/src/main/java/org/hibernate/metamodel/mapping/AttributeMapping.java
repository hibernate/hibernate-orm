/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.tuple.ValueGeneration;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.MutabilityPlanExposer;

/**
 * Describes an attribute at the mapping model level.
 *
 * @author Steve Ebersole
 */
public interface AttributeMapping extends ModelPart, ValueMapping, Fetchable, PropertyBasedMapping, MutabilityPlanExposer {
	String getAttributeName();

	@Override
	default String getPartName() {
		return getAttributeName();
	}

	AttributeMetadataAccess getAttributeMetadataAccess();

	ManagedMappingType getDeclaringType();

	/**
	 * The getter/setter access to this attribute
	 */
	PropertyAccess getPropertyAccess();

	/**
	 * Convenient access to getting the value for this attribute from the "owner"
	 */
	default Object getValue(Object container) {
		return getPropertyAccess().getGetter().get( container );
	}

	default void setValue(Object container, Object value) {
		getPropertyAccess().getSetter().set( container, value );
	}

	/**
	 * The value generation strategy to use for this attribute.
	 *
	 * @apiNote Only relevant for non-id attributes
	 */
	ValueGeneration getValueGeneration();

	@Override
	default EntityMappingType findContainingEntityMapping() {
		return getDeclaringType().findContainingEntityMapping();
	}

	@Override
	default MutabilityPlan<?> getExposedMutabilityPlan() {
		return getAttributeMetadataAccess().resolveAttributeMetadata( null ).getMutabilityPlan();
	}
}
