/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * Models an <tt>IS [NOT] EMPTY</tt> restriction
 *
 * @author Steve Ebersole
 */
public class EmptinessPredicate extends AbstractSimplePredicate {
	private final PluralPath<?> pluralPath;

	public EmptinessPredicate(
			PluralPath<?> pluralPath,
			CriteriaNodeBuilder builder) {
		this( pluralPath, false, builder );
	}

	public EmptinessPredicate(
			PluralPath<?> pluralPath,
			boolean negated,
			CriteriaNodeBuilder builder) {
		super( negated, builder );
		this.pluralPath = pluralPath;
	}

	public PluralPath<?> getPluralPath() {
		return pluralPath;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitEmptinessPredicate( this );
	}
}
