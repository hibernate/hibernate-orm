/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class SqmMemberOfPredicate extends AbstractNegatableSqmPredicate {
	private final SqmPath<?> pluralPath;

	public SqmMemberOfPredicate(SqmPath<?> pluralPath, NodeBuilder nodeBuilder) {
		this( pluralPath, false, nodeBuilder );
	}

	@SuppressWarnings("WeakerAccess")
	public SqmMemberOfPredicate(SqmPath pluralPath, boolean negated, NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );

		this.pluralPath = pluralPath;
	}

	public SqmPath<?> getPluralPath() {
		return pluralPath;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMemberOfPredicate( this );
	}
}
