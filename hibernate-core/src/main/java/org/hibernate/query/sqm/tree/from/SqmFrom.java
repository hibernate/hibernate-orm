/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;

/**
 * Models a Bindable's inclusion in the {@code FROM} clause.
 *
 * @author Steve Ebersole
 */
public interface SqmFrom {
	/**
	 * Obtain reference to the FromElementSpace that this FromElement belongs to.
	 */
	SqmFromElementSpace getContainingSpace();

	SqmNavigableReference getBinding();

	/**
	 * A unique identifier for this SqmFrom element  across all QuerySpecs (all
	 * AliasRegistry instances) for a given query.
	 * <p/>
	 * Can be used to locate a FromElement outside the context of a particular AliasRegistry.
	 *
	 * @see ParsingContext#globalFromElementMap
	 */
	String getUniqueIdentifier();

	/**
	 * Get the identification variable (alias) assigned to this FromElement.  If an explicit
	 * identification variable was given in the source query that identification variable is
	 * returned here; otherwise an implicit identification variable is generated and returned
	 * here.
	 * <p/>
	 * Note that the spec also sometimes calls this a "range variable", although it tends to
	 * limit this usage to just query space roots.
	 * <p/>
	 * Note2 : Never returns {@code null}; if the query did not specify an identification
	 * variable, one is implicitly generated.
	 */
	String getIdentificationVariable();

	/**
	 * Obtain the downcast target for cases where a downcast (treat) is defined in the
	 * directly in the from-clause where this FromElement is declared.  E.g
	 * <code>select b.isbn from Order o join treat(o.product as Book b)</code>; here
	 * the FromElement indicated by {@code join treat(o.product as Book b)} would have
	 * Book as an intrinsic subclass indicator.
	 *
	 * @todo - will need a wrapper approach to handle non-intrinsic attribute references
	 * 		^^ assuming attribute references expect SqmFrom objects as their "lhs"
	 */
	EntityValuedExpressableType getIntrinsicSubclassIndicator();

	<T> T accept(SemanticQueryWalker<T> walker);
}
