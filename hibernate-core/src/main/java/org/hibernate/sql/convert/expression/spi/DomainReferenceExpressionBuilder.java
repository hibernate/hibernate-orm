/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.expression.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.expression.domain.ColumnBindingSource;
import org.hibernate.sql.ast.expression.domain.DomainReferenceExpression;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.convert.internal.FromClauseIndex;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sqm.query.expression.domain.PluralAttributeBinding;
import org.hibernate.sqm.query.expression.domain.PluralAttributeElementBinding;
import org.hibernate.sqm.query.expression.domain.SingularAttributeBinding;

/**
 * @author Steve Ebersole
 */
public interface DomainReferenceExpressionBuilder {
	boolean isShallow();

	DomainReferenceExpression buildEntityExpression(
			BuildingContext buildingContext,
			ColumnBindingSource columnBindingSource,
			EntityPersister improvedEntityPersister,
			PropertyPath propertyPath);

	DomainReferenceExpression buildSingularAttributeExpression(
			BuildingContext buildingContext,
			SingularAttributeBinding singularAttributeBinding);

	DomainReferenceExpression buildPluralAttributeExpression(
			BuildingContext buildingContext,
			PluralAttributeBinding attributeBinding);

	DomainReferenceExpression buildPluralAttributeElementReferenceExpression(
			PluralAttributeElementBinding binding,
			TableGroup resolvedTableGroup,
			PropertyPath convert);

	interface BuildingContext {
		SessionFactoryImplementor getSessionFactory();
		FromClauseIndex getFromClauseIndex();
		ReturnResolutionContext getReturnResolutionContext();
		DomainReferenceExpressionBuilder getCurrentDomainReferenceExpressionBuilder();
		Clause getCurrentStatementClause();
	}
}
