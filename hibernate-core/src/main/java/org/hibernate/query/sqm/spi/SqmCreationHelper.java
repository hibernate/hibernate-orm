/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class SqmCreationHelper {
	public static NavigablePath buildRootNavigablePath(String base, String alias) {
		// Make sure we always create a unique alias, otherwise we might use a wrong table group for the same join
		return alias == null
				? new NavigablePath( base, Long.toString( System.nanoTime() ) )
				: new NavigablePath( base, alias );
	}

	public static NavigablePath buildSubNavigablePath(NavigablePath lhs, String base, String alias) {
		// Make sure we always create a unique alias, otherwise we might use a wrong table group for the same join
		return alias == null
				? lhs.append( base, Long.toString( System.nanoTime() ) )
				: lhs.append( base, alias );
	}

	public static NavigablePath buildSubNavigablePath(SqmPath<?> lhs, String subNavigable, String alias) {
		if ( lhs == null ) {
			throw new IllegalArgumentException(
					"`lhs` cannot be null for a sub-navigable reference - " + subNavigable
			);
		}
		NavigablePath navigablePath = lhs.getNavigablePath();
		if ( lhs.getReferencedPathSource() instanceof PluralPersistentAttribute<?, ?, ?> ) {
			navigablePath = navigablePath.append( CollectionPart.Nature.ELEMENT.getName() );
		}
		return buildSubNavigablePath( navigablePath, subNavigable, alias );
	}

	private SqmCreationHelper() {
	}

}
