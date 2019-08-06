/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.mapping.spi;

import java.util.Locale;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.type.Type;

/**
 * Describes a mapping related to any part of the app's domain model - e.g.
 * an attribute, an entity identifier, collection elements, etc
 *
 * NOTE : this does not include EntityPersister, which however is a ValueMappingContainer
 *
 * Used in generation of SQL AST
 *
 * @author Steve Ebersole
 */
public interface ValueMapping extends ModelPart {

	/**
	 * Get the Type associated with this mapping
	 */
	default Type getValueType() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Treat operation.  Asks the ValueMapping to treat itself as the
	 * given `targetType`, if it can.
	 *
	 * @apiNote This is not necessarily limited to things the ValueMapping
	 * itself implements.
	 */
	default <X> X as(Class<X> targetType) {
		if ( targetType.isInstance( this ) ) {
			//noinspection unchecked
			return (X) this;
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"`%s` cannot be treated as `%s`",
						getClass().getName(),
						targetType.getName()
				)
		);
	}
}
