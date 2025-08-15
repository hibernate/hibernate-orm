/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Set;

import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;

/**
 * @author Steve Ebersole
 */
public interface AttributeContainer<J> extends ManagedDomainType<J> {
	InFlightAccess<J> getInFlightAccess();

	/**
	 * Used during creation of the type
	 */
	interface InFlightAccess<J> {
		void addAttribute(PersistentAttribute<J,?> attribute);

		/**
		 * Callback used when we have a singular id attribute of some form - either a simple id
		 * or an aggregated composite id ({@link jakarta.persistence.EmbeddedId})
		 */
		default void applyIdAttribute(SingularPersistentAttribute<J, ?> idAttribute) {
			throw new UnsupportedMappingException(
					"AttributeContainer [" + getClass().getName() + "] does not support identifiers"
			);
		}

		default void applyNonAggregatedIdAttributes(
				Set<SingularPersistentAttribute<? super J, ?>> idAttributes,
				EmbeddableDomainType<?> idClassType) {
			throw new UnsupportedMappingException(
					"AttributeContainer [" + getClass().getName() + "] does not support identifiers"
			);
		}

		/**
		 * todo (6.0) : we still need to implement this properly and the contract may change
		 * 		- specifically I am not certain we will be able to re-use `SingularPersistentAttribute`
		 * 		because of its dependence on declaring-type, etc that we may not be able to do
		 */
		default void applyIdClassAttributes(Set<SingularPersistentAttribute<? super J, ?>> idClassAttributes) {
			throw new UnsupportedMappingException(
					"AttributeContainer [" + getClass().getName() + "] does not support identifiers"
			);
		}

		default void applyVersionAttribute(SingularPersistentAttribute<J, ?> versionAttribute) {
			throw new UnsupportedMappingException(
					"AttributeContainer [" + getClass().getName() + "] does not support versions"
			);
		}

		default void applyNaturalIdAttribute(PersistentAttribute<J, ?> versionAttribute) {
			throw new UnsupportedMappingException(
					"AttributeContainer [" + getClass().getName() + "] does not support natural ids"
			);
		}

		default void addConcreteGenericAttribute(PersistentAttribute<J, ?> idAttribute) {
			throw new UnsupportedMappingException(
					"AttributeContainer [" + getClass().getName() + "] does not generic embeddables"
			);
		}

		/**
		 * Called when configuration of the type is complete
		 */
		void finishUp();
	}
}
