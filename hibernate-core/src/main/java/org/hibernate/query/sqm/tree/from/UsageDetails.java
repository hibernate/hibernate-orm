/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import java.util.Collection;
import java.util.EnumSet;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * Details about how a SqmFrom is used in the query.
 *
 * @author Steve Ebersole
 */
public interface UsageDetails {
	/**
	 * Is it used in the from-clause?
	 *
	 * todo (6.0) : use this from visiting from-clause
	 */
	boolean isUsedInFrom();

	/**
	 * Is it used in the select-clause?
	 * <p>
	 * todo (6.0) : use this from visiting selections
	 */
	default boolean isUsedInSelect() {
		throw new NotYetImplementedFor6Exception(  );
	}

	/**
	 * An intrinsic subclass indicator is given when the SqmFrom
	 * is defined in the from-clause.  E.g.:
	 *
	 *     select c from Order o join treat( o.customer as ApacCustomer ) c
	 *
	 * here, the intrinsic subclass indicator for the `o.customer` join would
	 * be the `ApacCustomer` subclass of `Customer`
	 */
	ManagedTypeDescriptor getIntrinsicSubclassIndicator();

	/**
	 * An incidental subclass indicator is given when the SqmFrom
	 * is downcast when referenced outside the from-clause.  E.g.
	 *
	 *     select treat( o.customer as ApacCustomer ).someApacSpecificDetail from Order o
	 *
	 * todo (6.0) : another way to look at this is that this particular example also illustrates an intrinsic subclass indicator
	 * 		Maybe the keeping the combo of downcast-target and location is needed afterall.  that or
	 * 		allow intrinsic indicators to be multi-valued
	 */
	Collection<ManagedTypeDescriptor> getIncidentalSubclassIndicators();

	// todo (6.0) : is it important to "bundle" these downcast+location combos together?
	//		does it matter where which subclass downcast comes from?  different treats in different locations?

	/**
	 * The locations where downcasts happened
	 */
	EnumSet<DowncastLocation> getDowncastLocations();

	/**
	 * All of the "sub" navigables referenced from the described SqmFrom.  E.g.,
	 * given the `SqmRoot(Person p)` I'd get for `"... from Person p..."`,
	 * and given the selection `"select p.id, p.name, p.age from Person p..."` -
	 * that SqmRoot would contain `EntityIdentifier(id), SingularAttribute(name), SingularAttribute(age)`
	 * as its `referencedNavigables`.
	 */
	Collection<Navigable> getReferencedNavigables();
}
