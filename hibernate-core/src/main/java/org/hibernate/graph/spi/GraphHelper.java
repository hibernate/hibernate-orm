/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.spi;

import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.MapAttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.PluralAttributeImplementor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.SingularAttributeImplementor;

/**
 * Helper containing utilities useful for graph handling
 *
 * @author Steve Ebersole
 */
public class GraphHelper {
	@SuppressWarnings("unchecked")
	public static <J> SimpleTypeImplementor<J> resolveKeyTypeDescriptor(SingularAttributeImplementor attribute) {
		// only valid for entity-valued attributes where the entity has a
		// composite id
		final SimpleTypeImplementor attributeType = attribute.getType();
		if ( attributeType instanceof IdentifiableTypeImplementor  ) {
			return ( (IdentifiableTypeImplementor) attributeType ).getIdType();
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public static <J> SimpleTypeImplementor<J> resolveKeyTypeDescriptor(PluralAttributeImplementor attribute) {
		if ( attribute instanceof SingularAttributeImplementor ) {
			// only valid for entity-valued attributes where the entity has a
			// composite id
			final SimpleTypeImplementor attributeType = ( (SingularAttributeImplementor) attribute ).getType();
			if ( attributeType instanceof IdentifiableTypeImplementor  ) {
				return ( (IdentifiableTypeImplementor) attributeType ).getIdType();
			}

			return null;
		}
		else if ( attribute instanceof PluralAttributeImplementor ) {
			if ( attribute instanceof MapAttributeImplementor ) {
				return ( (MapAttributeImplementor) attribute ).getKeyType();
			}

			return null;
		}

		throw new IllegalArgumentException(
				"Unexpected Attribute Class [" + attribute.getClass().getName()
						+ "] - expecting SingularAttributeImplementor or PluralAttributeImplementor"
		);
	}
}
