/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;
import java.util.Set;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import org.hibernate.query.common.FetchClauseType;

/**
 * Models a {@code SELECT} query.  Used as a delegate in
 * implementing {@link jakarta.persistence.criteria.CriteriaQuery}
 * and {@link jakarta.persistence.criteria.Subquery}.
 *
 * @apiNote Internally (HQL and SQM) Hibernate supports ordering and limiting
 * for both root- and sub- criteria even though JPA only defines support for
 * them on a root.
 *
 * @see JpaCriteriaQuery
 * @see JpaSubQuery
 *
 * @author Steve Ebersole
 */
public interface JpaQueryStructure<T> extends JpaQueryPart<T> {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Select clause

	boolean isDistinct();

	JpaQueryStructure<T> setDistinct(boolean distinct);

	JpaSelection<T> getSelection();

	JpaQueryStructure<T> setSelection(JpaSelection<T> selection);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// From clause

	Set<? extends JpaRoot<?>> getRoots();

	List<? extends JpaRoot<?>> getRootList();

	JpaQueryStructure<T> addRoot(JpaRoot<?> root);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Where clause

	JpaPredicate getRestriction();

	JpaQueryStructure<T> setRestriction(JpaPredicate restriction);

	JpaQueryStructure<T> setRestriction(Expression<Boolean> restriction);

	JpaQueryStructure<T> setRestriction(Predicate... restrictions);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Grouping (group-by / having) clause

	List<? extends JpaExpression<?>> getGroupingExpressions();

	JpaQueryStructure<T> setGroupingExpressions(List<? extends JpaExpression<?>> grouping);

	JpaQueryStructure<T> setGroupingExpressions(JpaExpression<?>... grouping);

	JpaPredicate getGroupRestriction();

	JpaQueryStructure<T> setGroupRestriction(JpaPredicate restrictions);

	JpaQueryStructure<T> setGroupRestriction(Expression<Boolean> restriction);

	JpaQueryStructure<T> setGroupRestriction(Predicate... restrictions);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant overrides

	@Override
	JpaQueryStructure<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications);

	@Override
	JpaQueryStructure<T> setOffset(JpaExpression<? extends Number> offset);

	@Override
	JpaQueryStructure<T> setFetch(JpaExpression<? extends Number> fetch);

	@Override
	JpaQueryStructure<T> setFetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType);
}
