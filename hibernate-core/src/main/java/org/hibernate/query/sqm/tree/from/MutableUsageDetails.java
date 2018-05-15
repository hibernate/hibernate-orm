/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * @author Steve Ebersole
 */
public interface MutableUsageDetails extends UsageDetails {
	/**
	 * Marks the SqmFrom as being "used" in the from-clause, although that
	 * really indicates that it was defined/created in the from-clause too
	 */
	void usedInFromClause();

	/**
	 * todo (6.0) : figure out how to best handle calling this
	 *
	 * -> is it enough to do it just for the actually selected NavigableReference?
	 * 	or should it be set for any of the "intermediate" NavigableReferences (joins) too?
	 *
	 * 		select o.customer from Order o
	 * 		select o.customer.shippingAddress from Order o
	 *
	 * 	In the first, clearly `o.customer` is "used in the select".  But what
	 * 	about the second?  Is `o.customer` "used in the select" here too?  If so (and
	 * 	I think that is correct) then it cannot be as simple as calling it on the
	 * 	final navigable reference selected - it has to be called on all of its
	 * 	containers too.
	 *
	 * 	Anyway... how to best handle this...?
	 */
	void usedInSelectClause();

	/**
	 * todo (6.0) : implement appropriate calls to this
	 * todo (6.0) : this could be used to handle implicit downcasts
	 */
	void addReferencedNavigable(Navigable navigable);

	/**
	 * todo (6.0) : a `ManagedTypeValuedNavigable` would be better (to allow for composites)
	 */
	void addDownCast(
			boolean intrinsic,
			ManagedTypeDescriptor downcastType,
			DowncastLocation downcastLocation);
}
