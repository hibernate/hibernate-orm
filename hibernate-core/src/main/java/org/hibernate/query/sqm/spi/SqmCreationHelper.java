/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Steve Ebersole
 */
public class SqmCreationHelper {

	/**
	 * This is a special alias that we use for implicit joins within the FROM clause.
	 * Passing this alias will cause that we don't generate a unique alias for a path,
	 * but instead use a <code>null</code> alias.
	 *
	 * The effect of this is, that we use the same table group for a query like
	 * `... exists ( from alias.intermediate.attribute where alias.intermediate.otherAttribute is not null )`
	 * for the path in the FROM clause and the one in the WHERE clause.
	 */
	public static final String IMPLICIT_ALIAS = "{implicit}";

	private static final AtomicLong UNIQUE_ID_COUNTER = new AtomicLong();

	public static NavigablePath buildRootNavigablePath(String base, String alias) {
		return new NavigablePath( base, determineAlias( alias ) );
	}

	public static NavigablePath buildSubNavigablePath(NavigablePath lhs, String base, String alias) {
		return lhs.append( base, determineAlias( alias ) );
	}

	public static String acquireUniqueAlias() {
		return Long.toString(UNIQUE_ID_COUNTER.incrementAndGet());
	}

	public static String determineAlias(String alias) {
		// Make sure we always create a unique alias, otherwise we might use a wrong table group for the same join
		if ( alias == null ) {
			return acquireUniqueAlias();
		}
		else if ( alias == IMPLICIT_ALIAS ) {
			return null;
		}
		return alias;
	}

	public static NavigablePath buildSubNavigablePath(SqmPath<?> lhs, String subNavigable, String alias) {
		if ( lhs == null ) {
			throw new IllegalArgumentException(
					"`lhs` cannot be null for a sub-navigable reference - " + subNavigable
			);
		}
		NavigablePath navigablePath = lhs.getNavigablePath();
		if ( lhs.getResolvedModel() instanceof PluralPersistentAttribute<?, ?, ?>
				&& CollectionPart.Nature.fromName( subNavigable ) == null ) {
			navigablePath = navigablePath.append( CollectionPart.Nature.ELEMENT.getName() );
		}
		return buildSubNavigablePath( navigablePath, subNavigable, alias );
	}

	private SqmCreationHelper() {
	}

}
