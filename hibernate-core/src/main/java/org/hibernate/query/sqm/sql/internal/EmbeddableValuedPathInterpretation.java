/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTreatedPath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.expression.SqlTupleContainer;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.update.Assignable;

/**
 * @author Steve Ebersole
 */
public class EmbeddableValuedPathInterpretation<T> extends AbstractSqmPathInterpretation<T> implements Assignable, SqlTupleContainer {

	/**
	 * Static factory
	 */
	public static <T> Expression from(
			SqmEmbeddedValuedSimplePath<T> sqmPath,
			SqmToSqlAstConverter converter,
			SemanticQueryWalker<?> sqmWalker,
			boolean jpaQueryComplianceEnabled,
			Clause currentClause) {
		TableGroup tableGroup = converter.getFromClauseAccess().findTableGroup( sqmPath.getLhs().getNavigablePath() );

		EntityMappingType treatTarget = null;
		if ( jpaQueryComplianceEnabled ) {
			final MappingMetamodel mappingMetamodel = converter.getCreationContext()
					.getSessionFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel();
			if ( sqmPath.getLhs() instanceof SqmTreatedPath ) {
				//noinspection rawtypes
				final EntityDomainType<?> treatTargetDomainType = ( (SqmTreatedPath) sqmPath.getLhs() ).getTreatTarget();
				treatTarget = mappingMetamodel.findEntityDescriptor( treatTargetDomainType.getHibernateEntityName() );
			}
			else if ( sqmPath.getLhs().getNodeType() instanceof EntityDomainType ) {
				//noinspection rawtypes
				final EntityDomainType<?> entityDomainType = (EntityDomainType) sqmPath.getLhs().getNodeType();
				treatTarget = mappingMetamodel.findEntityDescriptor( entityDomainType.getHibernateEntityName() );

			}
		}

		final ModelPartContainer modelPart = tableGroup.getModelPart();
		final EmbeddableValuedModelPart mapping;
		// In the select, group by, order by and having clause we have to make sure we render the column of the target table,
		// never the FK column, if the lhs is a SqmFrom i.e. something explicitly queried/joined.
		if ( ( currentClause == Clause.GROUP || currentClause == Clause.SELECT || currentClause == Clause.ORDER || currentClause == Clause.HAVING )
				&& sqmPath.getLhs() instanceof SqmFrom<?, ?>
				&& modelPart.getPartMappingType() instanceof ManagedMappingType ) {
			mapping = (EmbeddableValuedModelPart) ( (ManagedMappingType) modelPart.getPartMappingType() ).findSubPart(
					sqmPath.getReferencedPathSource().getPathName(),
					treatTarget
			);
		}
		else {
			mapping = (EmbeddableValuedModelPart) modelPart.findSubPart(
					sqmPath.getReferencedPathSource().getPathName(),
					treatTarget
			);
		}

		return new EmbeddableValuedPathInterpretation<>(
				mapping.toSqlExpression(
						tableGroup,
						converter.getCurrentClauseStack().getCurrent(),
						converter,
						converter
				),
				sqmPath.getNavigablePath(),
				mapping,
				tableGroup
		);
	}

	private final SqlTuple sqlExpression;

	public EmbeddableValuedPathInterpretation(
			SqlTuple sqlExpression,
			NavigablePath navigablePath,
			EmbeddableValuedModelPart mapping,
			TableGroup tableGroup) {
		super( navigablePath, mapping, tableGroup );
		this.sqlExpression = sqlExpression;
	}

	@Override
	public SqlTuple getSqlExpression() {
		return sqlExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlExpression.accept( sqlTreeWalker );
	}

	@Override
	public String toString() {
		return "EmbeddableValuedPathInterpretation(" + getNavigablePath() + ")";
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		for ( Expression expression : sqlExpression.getExpressions() ) {
			if ( !( expression instanceof ColumnReference ) ) {
				throw new IllegalArgumentException( "Expecting ColumnReference, found : " + expression );
			}
			columnReferenceConsumer.accept( (ColumnReference) expression );
		}
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		final List<ColumnReference> results = new ArrayList<>();
		visitColumnReferences( results::add );
		return results;
	}

	@Override
	public SqlTuple getSqlTuple() {
		return sqlExpression;
	}
}
