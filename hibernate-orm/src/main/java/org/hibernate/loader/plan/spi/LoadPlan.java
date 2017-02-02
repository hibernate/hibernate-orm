/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import java.util.List;

/**
 * Describes a plan for performing a load of results.
 *
 * Generally speaking there are 3 forms of load plans:<ul>
 *     <li>
 *         {@link org.hibernate.loader.plan.spi.LoadPlan.Disposition#ENTITY_LOADER} - An entity load plan for
 *         handling get/load handling.  This form will typically have a single return (of type {@link org.hibernate.loader.plan.spi.EntityReturn})
 *         defined by {@link #getReturns()}, possibly defining fetches.
 *     </li>
 *     <li>
 *         {@link org.hibernate.loader.plan.spi.LoadPlan.Disposition#COLLECTION_INITIALIZER} - A collection initializer,
 *         used to load the contents of a collection.  This form will typically have a single return (of
 *         type {@link org.hibernate.loader.plan.spi.CollectionReturn}) defined by {@link #getReturns()}, possibly defining fetches
 *     </li>
 *     <li>
 *         {@link org.hibernate.loader.plan.spi.LoadPlan.Disposition#MIXED} - A query load plan which can contain
 *         multiple returns of mixed type (though all implementing {@link org.hibernate.loader.plan.spi.Return}).  Again, may possibly define fetches.
 *     </li>
 * </ul>
 * <p/>
 * todo : would also like to see "call back" style access for handling "subsequent actions" such as...<ul>
 *     <li>follow-on locking</li>
 *     <li>join fetch conversions to subselect fetches</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface LoadPlan {

	/**
	 * What is the disposition of this LoadPlan, in terms of its returns.
	 *
	 * @return The LoadPlan's disposition
	 */
	public Disposition getDisposition();

	/**
	 * Get the returns indicated by this LoadPlan.<ul>
	 *     <li>
	 *         A {@link Disposition#ENTITY_LOADER} LoadPlan would have just a single Return of type {@link org.hibernate.loader.plan.spi.EntityReturn}.
	 *     </li>
	 *     <li>
	 *         A {@link Disposition#COLLECTION_INITIALIZER} LoadPlan would have just a single Return of type
	 *         {@link org.hibernate.loader.plan.spi.CollectionReturn}.
	 *     </li>
	 *     <li>
	 *         A {@link Disposition#MIXED} LoadPlan would contain a mix of {@link org.hibernate.loader.plan.spi.EntityReturn} and
	 *         {@link org.hibernate.loader.plan.spi.ScalarReturn} elements, but no {@link org.hibernate.loader.plan.spi.CollectionReturn}.
	 *     </li>
	 * </ul>
	 * <p/>
	 * When generating SQL, the Returns provide columns/formulas used in the "select clause".
	 *
	 * @return The Returns for this LoadPlan.
	 *
	 * @see Disposition
	 */
	public List<? extends Return> getReturns();

	/**
	 * Gets the {@link QuerySpaces} for the load plan, which contains a {@link QuerySpace}
	 * reference for each non-scalar return and for each entity, collection, and composite
	 * {@link FetchSource}.
	 * <p/>
	 * When generating SQL, the query spaces provide data for the "from clause" including joins.
	 *
	 * @return The QuerySpaces
	 */
	public QuerySpaces getQuerySpaces();

	/**
	 * Does this load plan indicate that lazy attributes are to be force fetched?
	 * <p/>
	 * Here we are talking about laziness in regards to the legacy bytecode enhancement which adds support for
	 * partial selects of an entity's state (e.g., skip loading a lob initially, wait until/if it is needed)
	 * <p/>
	 * This one would effect the SQL that needs to get generated as well as how the result set would be read.
	 * Therefore we make this part of the LoadPlan contract.
	 * <p/>
	 * NOTE that currently this is only relevant for HQL loaders when the HQL has specified the {@code FETCH ALL PROPERTIES}
	 * key-phrase.  In all other cases, this returns false.

	 * @return Whether or not to
	 */
	public boolean areLazyAttributesForceFetched();

	/**
	 * Convenient form of checking {@link #getReturns()} for scalar root returns.
	 *
	 * @return {@code true} if {@link #getReturns()} contained any scalar returns; {@code false} otherwise.
	 */
	public boolean hasAnyScalarReturns();

	/**
	 * Enumerated possibilities for describing the disposition of this LoadPlan.
	 */
	public static enum Disposition {
		/**
		 * This is an "entity loader" load plan, which describes a plan for loading one or more entity instances of
		 * the same entity type.  There is a single return, which will be of type {@link org.hibernate.loader.plan.spi.EntityReturn}
		 */
		ENTITY_LOADER,
		/**
		 * This is a "collection initializer" load plan, which describes a plan for loading one or more entity instances of
		 * the same collection type.  There is a single return, which will be of type {@link org.hibernate.loader.plan.spi.CollectionReturn}
		 */
		COLLECTION_INITIALIZER,
		/**
		 * We have a mixed load plan, which will have one or more returns of {@link org.hibernate.loader.plan.spi.EntityReturn}
		 * and {@link org.hibernate.loader.plan.spi.ScalarReturn} (NOT {@link org.hibernate.loader.plan.spi.CollectionReturn}).
		 */
		MIXED
	}
}
