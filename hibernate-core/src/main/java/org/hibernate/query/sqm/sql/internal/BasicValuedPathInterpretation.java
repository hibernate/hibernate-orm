/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.UnknownPathException;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;

import static org.hibernate.query.sqm.internal.SqmUtil.getActualTableGroup;

/**
 * @author Steve Ebersole
 */
public class BasicValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements Assignable,  DomainResultProducer<T> {
	/**
	 * Static factory
	 */
	public static <T> BasicValuedPathInterpretation<T> from(
			SqmBasicValuedSimplePath<T> sqmPath,
			SqmToSqlAstConverter sqlAstCreationState,
			boolean jpaQueryComplianceEnabled) {
		final SqmPath<?> lhs = sqmPath.getLhs();
		final TableGroup tableGroup = getActualTableGroup(
				sqlAstCreationState.getFromClauseAccess().getTableGroup( lhs.getNavigablePath() ),
				sqmPath
		);
		EntityMappingType treatTarget = null;
		final ModelPartContainer modelPartContainer;
		if ( lhs instanceof SqmTreatedPath<?, ?> ) {
			final EntityDomainType<?> treatTargetDomainType = ( (SqmTreatedPath<?, ?>) lhs ).getTreatTarget();

			final MappingMetamodel mappingMetamodel = sqlAstCreationState.getCreationContext()
					.getSessionFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel();
			final EntityPersister treatEntityDescriptor = mappingMetamodel.findEntityDescriptor( treatTargetDomainType.getHibernateEntityName() );
			final MappingType tableGroupMappingType = tableGroup.getModelPart().getPartMappingType();
			if ( tableGroupMappingType instanceof EntityMappingType
					&& treatEntityDescriptor.isTypeOrSuperType( (EntityMappingType) tableGroupMappingType ) ) {
				modelPartContainer = tableGroup.getModelPart();
				treatTarget = treatEntityDescriptor;
			}
			else {
				modelPartContainer = treatEntityDescriptor;
			}
		}
		else {
			modelPartContainer = tableGroup.getModelPart();
			if ( jpaQueryComplianceEnabled && lhs.getNodeType() instanceof EntityDomainType<?> ) {
				final EntityDomainType<?> entityDomainType = (EntityDomainType<?>) lhs.getNodeType();
				final MappingMetamodel mappingMetamodel = sqlAstCreationState.getCreationContext()
						.getSessionFactory()
						.getRuntimeMetamodels()
						.getMappingMetamodel();
				treatTarget = mappingMetamodel.findEntityDescriptor( entityDomainType.getHibernateEntityName() );
			}
		}

		final BasicValuedModelPart mapping;
		// In the select, group by, order by and having clause we have to make sure we render the column of the target table,
		// never the FK column, if the lhs is a SqmFrom i.e. something explicitly queried/joined
		// and if this basic path is part of the group by clause
		final Clause currentClause = sqlAstCreationState.getCurrentClauseStack().getCurrent();
		final SqmQueryPart<?> sqmQueryPart = sqlAstCreationState.getCurrentSqmQueryPart();
		if ( ( currentClause == Clause.GROUP || currentClause == Clause.SELECT || currentClause == Clause.ORDER || currentClause == Clause.HAVING )
				&& lhs instanceof SqmFrom<?, ?>
				&& modelPartContainer.getPartMappingType() instanceof ManagedMappingType
				&& sqmQueryPart.isSimpleQueryPart()
				&& sqmQueryPart.getFirstQuerySpec().groupByClauseContains( sqmPath.getNavigablePath() ) ) {
			mapping = (BasicValuedModelPart) ( (ManagedMappingType) modelPartContainer.getPartMappingType() ).findSubPart(
					sqmPath.getReferencedPathSource().getPathName(),
					treatTarget
			);
		}
		else {
			mapping = (BasicValuedModelPart) modelPartContainer.findSubPart(
					sqmPath.getReferencedPathSource().getPathName(),
					treatTarget
			);
		}

		if ( mapping == null ) {
			if ( jpaQueryComplianceEnabled ) {
				// to get the better error, see if we got nothing because of treat handling
				final ModelPart subPart = tableGroup.getModelPart().findSubPart(
						sqmPath.getReferencedPathSource().getPathName(),
						null
				);
				if ( subPart != null ) {
					throw new StrictJpaComplianceViolation( StrictJpaComplianceViolation.Type.IMPLICIT_TREAT );
				}
			}

			throw new UnknownPathException( "Path '" + sqmPath.getNavigablePath() + "' did not reference a known model part" );
		}

		final TableReference tableReference = tableGroup.resolveTableReference(
				sqmPath.getNavigablePath(),
				mapping,
				mapping.getContainingTableExpression()
		);

		final Expression expression = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
				tableReference,
				mapping
		);

		final ColumnReference columnReference;
		if ( expression instanceof ColumnReference ) {
			columnReference = ( (ColumnReference) expression );
		}
		else if ( expression instanceof SqlSelectionExpression ) {
			final Expression selectedExpression = ( (SqlSelectionExpression) expression ).getSelection().getExpression();
			assert selectedExpression instanceof ColumnReference;
			columnReference = (ColumnReference) selectedExpression;
		}
		else {
			throw new UnsupportedOperationException( "Unsupported basic-valued path expression : " + expression );
		}

		return new BasicValuedPathInterpretation<>( columnReference, sqmPath.getNavigablePath(), mapping, tableGroup );
	}

	private final ColumnReference columnReference;

	public BasicValuedPathInterpretation(
			ColumnReference columnReference,
			NavigablePath navigablePath,
			BasicValuedModelPart mapping,
			TableGroup tableGroup) {
		super( navigablePath, mapping, tableGroup );
		assert columnReference != null;
		this.columnReference = columnReference;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultProducer

	@Override
	public Expression getSqlExpression() {
		return columnReference;
	}

	@Override
	public ColumnReference getColumnReference() {
		return columnReference;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		columnReference.accept( sqlTreeWalker );
	}

	@Override
	public String toString() {
		return "BasicValuedPathInterpretation(" + getNavigablePath() + ")";
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		columnReferenceConsumer.accept( columnReference );
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		return Collections.singletonList( columnReference );
	}
}
