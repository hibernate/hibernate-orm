/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Supplier;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;

/**
 * Extension for SqmExpression implementations whose type can be inferred from
 * their surroundings.  E.g., consider an HQL fragment like
 * `... where e.status = :status...` where `e.status` is reference to a mapped
 * enumerated attribute.  The parameter would have no associated type, but its
 * usage in a comparison with `e.status` implies that the parameter type ought
 * to be the same as that `status` attribute.  This is used to handle cases where
 * we ought to apply value conversions based on this implied type.  E.g., in this
 * example, assuming `status` is an `Enumerated` attribute, we would need to apply
 * the same value conversion (named/ordinal) for the parameter as we would for
 * `status`.
 *
 * @implNote This is mandated by JPA in certain usage contexts (surroundings).
 * Hibernate supports a far greater set of usage contexts.
 *
 * todo (6.0) : document these various contexts ^^
 *
 * @author Steve Ebersole
 */
public interface InferableTypeSqmExpression extends SqmExpression {
	/**
	 * Injects a Supplier which can be used to later to find out the
	 * ExpressableType (if any) implied by this expressions "usage context"
	 *
	 * @param inference The implied type inference
	 */
	void impliedType(Supplier<? extends ExpressableType> inference);
}
