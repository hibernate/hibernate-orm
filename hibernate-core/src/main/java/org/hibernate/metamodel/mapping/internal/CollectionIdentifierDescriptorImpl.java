/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.internal.domain.basic.BasicResult;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.BasicType;

/**
 * @author Steve Ebersole
 */
public class CollectionIdentifierDescriptorImpl implements CollectionIdentifierDescriptor {
	private final CollectionPersister collectionDescriptor;
	private final String columnName;
	private final BasicType type;

	public CollectionIdentifierDescriptorImpl(
			CollectionPersister collectionDescriptor,
			String columnName,
			BasicType type) {
		this.collectionDescriptor = collectionDescriptor;
		this.columnName = columnName;
		this.type = type;
	}

	@Override
	public DomainResult createDomainResult(
			NavigablePath collectionPath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {


		final SqlAstCreationState astCreationState = creationState.getSqlAstCreationState();
		final SqlAstCreationContext astCreationContext = astCreationState.getCreationContext();
		final SessionFactoryImplementor sessionFactory = astCreationContext.getSessionFactory();
		final SqlExpressionResolver sqlExpressionResolver = astCreationState.getSqlExpressionResolver();

		final SqlSelection sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						SqlExpressionResolver.createColumnReferenceKey(
								tableGroup.getPrimaryTableReference(),
								columnName
						),
						p -> new ColumnReference(
								tableGroup.getPrimaryTableReference().getIdentificationVariable(),
								columnName,
								type,
								sessionFactory
						)
				),
				type.getJavaTypeDescriptor(),
				sessionFactory.getTypeConfiguration()
		);

		//noinspection unchecked
		return new BasicResult(
				sqlSelection.getValuesArrayPosition(),
				null,
				type.getJavaTypeDescriptor(),
				collectionPath
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + collectionDescriptor.getRole() + ")";
	}
}
