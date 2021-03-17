/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.spi;

import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;

/**
 * Helper containing utilities useful for graph handling
 *
 * @author Steve Ebersole
 */
public class GraphHelper {
	@SuppressWarnings("unchecked")
	public static <J> SimpleTypeDescriptor<J> resolveKeyTypeDescriptor(SingularPersistentAttribute attribute) {
		// only valid for entity-valued attributes where the entity has a
		// composite id
		final SimpleTypeDescriptor attributeType = attribute.getType();
		if ( attributeType instanceof IdentifiableTypeDescriptor ) {
			return ( (IdentifiableTypeDescriptor) attributeType ).getIdType();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static <J> SimpleTypeDescriptor<J> resolveKeyTypeDescriptor(PluralPersistentAttribute attribute) {
		if ( attribute instanceof SingularPersistentAttribute ) {
			// only valid for entity-valued attributes where the entity has a
			// composite id
			final SimpleTypeDescriptor attributeType = ( (SingularPersistentAttribute) attribute ).getType();
			if ( attributeType instanceof IdentifiableTypeDescriptor ) {
				return ( (IdentifiableTypeDescriptor) attributeType ).getIdType();
			}

			return null;
		}
		else if ( attribute instanceof PluralPersistentAttribute ) {
			if ( attribute instanceof MapPersistentAttribute ) {
				return ( (MapPersistentAttribute) attribute ).getKeyType();
			}

			return null;
		}

		throw new IllegalArgumentException(
				"Unexpected Attribute Class [" + attribute.getClass().getName()
						+ "] - expecting SingularAttributeImplementor or PluralAttributeImplementor"
		);
	}
}
