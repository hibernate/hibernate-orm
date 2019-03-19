/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class MemberOfSqmPredicate extends AbstractNegatableSqmPredicate {
	private final SqmPath pluralPath;

	public MemberOfSqmPredicate(SqmPath pluralPath) {
		this( pluralPath, false );
	}

	public MemberOfSqmPredicate(SqmPath pluralPath, boolean negated) {
		super( negated );

		this.pluralPath = pluralPath;
	}

	public SqmPath getPluralPath() {
		return pluralPath;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMemberOfPredicate( this );
	}
}
