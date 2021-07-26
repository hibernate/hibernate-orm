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
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;

/**
 * @author Steve Ebersole
 */
public class BasicValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements Assignable,  DomainResultProducer<T> {
	/**
	 * Static factory
	 */
	public static <T> BasicValuedPathInterpretation<T> from(
			SqmBasicValuedSimplePath<T> sqmPath,
			SqlAstCreationState sqlAstCreationState,
			SemanticQueryWalker sqmWalker,
			boolean jpaQueryComplianceEnabled) {
		TableGroup tableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( sqmPath.getLhs().getNavigablePath() );

		EntityMappingType treatTarget = null;
		if ( jpaQueryComplianceEnabled ) {
			if ( sqmPath.getLhs() instanceof SqmTreatedPath ) {
				final EntityDomainType treatTargetDomainType = ( (SqmTreatedPath) sqmPath.getLhs() ).getTreatTarget();
				final MappingMetamodel domainModel = sqlAstCreationState.getCreationContext().getDomainModel();
				treatTarget = domainModel.findEntityDescriptor( treatTargetDomainType.getHibernateEntityName() );
			}
			else if ( sqmPath.getLhs().getNodeType() instanceof EntityDomainType ) {
				final EntityDomainType entityDomainType = (EntityDomainType) sqmPath.getLhs().getNodeType();
				final MappingMetamodel domainModel = sqlAstCreationState.getCreationContext().getDomainModel();
				treatTarget = domainModel.findEntityDescriptor( entityDomainType.getHibernateEntityName() );
			}
		}

		final BasicValuedModelPart mapping = (BasicValuedModelPart) tableGroup.getModelPart().findSubPart(
				sqmPath.getReferencedPathSource().getPathName(),
				treatTarget
		);

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

			throw new SemanticException( "`" + sqmPath.getNavigablePath().getFullPath() + "` did not reference a known model part" );
		}

		final TableReference tableReference = tableGroup.resolveTableReference(
				sqmPath.getNavigablePath(),
				mapping.getContainingTableExpression()
		);

		final Expression expression = sqlAstCreationState.getSqlExpressionResolver().resolveSqlExpression(
				SqlExpressionResolver.createColumnReferenceKey(
						tableReference,
						mapping.getSelectionExpression()
				),
				sacs -> new ColumnReference(
						tableReference.getIdentificationVariable(),
						mapping,
						sqlAstCreationState.getCreationContext().getSessionFactory()
				)
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

	private BasicValuedPathInterpretation(
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
	public void accept(SqlAstWalker sqlTreeWalker) {
		columnReference.accept( sqlTreeWalker );
	}

	@Override
	public String toString() {
		return "BasicValuedPathInterpretation(" + getNavigablePath().getFullPath() + ')';
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
