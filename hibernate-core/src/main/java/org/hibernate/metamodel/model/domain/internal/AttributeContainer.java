/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Set;

import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;

/**
 * @author Steve Ebersole
 */
public interface AttributeContainer<J> {
	InFlightAccess<J> getInFlightAccess();

	/**
	 * Used during creation of the type
	 */
	interface InFlightAccess<J> {
		void addAttribute(PersistentAttribute<J,?,?> attribute);

		default void applyIdAttribute(SingularPersistentAttribute<J, ?> idAttribute) {
			throw new UnsupportedOperationException(
					"AttributeContainer [" + getClass().getName() + "] does not support identifiers"
			);
		}

		default void applyIdClassAttributes(Set<SingularPersistentAttribute<? super J, ?>> idClassAttributes) {
			throw new UnsupportedOperationException(
					"AttributeContainer [" + getClass().getName() + "] does not support identifiers"
			);
		}

		default void applyVersionAttribute(SingularPersistentAttribute<J, ?> versionAttribute) {
			throw new UnsupportedOperationException(
					"AttributeContainer [" + getClass().getName() + "] does not support versions"
			);
		}


		/**
		 * Called when configuration of the type is complete
		 */
		void finishUp();
	}
}
