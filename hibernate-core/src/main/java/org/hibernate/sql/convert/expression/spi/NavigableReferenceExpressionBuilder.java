/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.expression.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.sql.tree.Clause;
import org.hibernate.sql.tree.expression.domain.ColumnBindingSource;
import org.hibernate.sql.tree.expression.domain.NavigableReferenceExpression;
import org.hibernate.sql.tree.from.TableGroup;
import org.hibernate.sql.convert.internal.FromClauseIndex;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;

/**
 * @author Steve Ebersole
 */
public interface NavigableReferenceExpressionBuilder {
	boolean isShallow();

	NavigableReferenceExpression buildEntityExpression(
			BuildingContext buildingContext,
			ColumnBindingSource columnBindingSource,
			EntityPersister improvedEntityPersister,
			NavigablePath navigablePath);

	NavigableReferenceExpression buildSingularAttributeExpression(
			BuildingContext buildingContext,
			SqmSingularAttributeReference singularAttributeReference);

	NavigableReferenceExpression buildPluralAttributeExpression(
			BuildingContext buildingContext,
			SqmPluralAttributeReference pluralAttributeReference);

	NavigableReferenceExpression buildPluralAttributeElementReferenceExpression(
			SqmCollectionElementReference collectionElementReference,
			TableGroup resolvedTableGroup,
			NavigablePath navigablePath);

	interface BuildingContext {
		SessionFactoryImplementor getSessionFactory();
		FromClauseIndex getFromClauseIndex();
		ReturnResolutionContext getReturnResolutionContext();
		NavigableReferenceExpressionBuilder getCurrentDomainReferenceExpressionBuilder();
		Clause getCurrentStatementClause();
	}
}
