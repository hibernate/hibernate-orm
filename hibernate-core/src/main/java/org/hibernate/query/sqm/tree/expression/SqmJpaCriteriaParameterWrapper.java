/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.internal.DomainModelHelper;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;

/**
 * @author Steve Ebersole
 */
public class SqmJpaCriteriaParameterWrapper<T>
		extends AbstractSqmExpression<T>
		implements SqmParameter<T> {
	private final JpaCriteriaParameter<T> jpaCriteriaParameter;

	public SqmJpaCriteriaParameterWrapper(
			AllowableParameterType<T> type,
			JpaCriteriaParameter<T> jpaCriteriaParameter,
			NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.jpaCriteriaParameter = jpaCriteriaParameter;
	}

	@Override
	public String getName() {
		return jpaCriteriaParameter.getName();
	}

	@Override
	public Integer getPosition() {
		// for criteria anyway, these cannot be positional
		return null;
	}

	public JpaCriteriaParameter<T> getJpaCriteriaParameter() {
		return jpaCriteriaParameter;
	}

	@Override
	public AllowableParameterType<T> getNodeType() {
		if ( super.getNodeType() instanceof AllowableParameterType ) {
			return ( (AllowableParameterType<T>) super.getNodeType() );
		}

		throw new IllegalStateException( "Expecting AllowableParameterType as node type" );
	}

	@Override
	public Class<T> getParameterType() {
		return jpaCriteriaParameter.getParameterType();
	}

	@Override
	public boolean allowMultiValuedBinding() {
		return jpaCriteriaParameter.allowsMultiValuedBinding();
	}

	@Override
	public AllowableParameterType<T> getAnticipatedType() {
		return getNodeType();
	}

	@Override
	public SqmParameter copy() {
		return new SqmJpaCriteriaParameterWrapper<>(
				getNodeType(),
				jpaCriteriaParameter,
				nodeBuilder()
		);
	}

	/**
	 * Unsupported.  Visitation for a criteria parameter should be handled
	 * as part of {@link SemanticQueryWalker#visitJpaCriteriaParameter}.  This wrapper
	 * is intended just for representing unique SqmParameter references for each
	 * JpaCriteriaParameter occurence in the SQM true as part of the {@link org.hibernate.query.QueryParameter}
	 * -> {@link SqmParameter} -> {@link org.hibernate.sql.exec.spi.JdbcParameter} transformation.
	 * Each occurrence requires a unique SqmParameter to make sure we ultimately get the complete
	 * set of JdbcParameter references
	 */
	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		throw new UnsupportedOperationException(
				"Direct SemanticQueryWalker visitation of a SqmJpaCriteriaParameterWrapper " +
						"is not supported.  Visitation for a criteria parameter should be handled " +
						"during `SemanticQueryWalker#visitJpaCriteriaParameter`.  This wrapper is " +
						"intended only for representing unique SQM parameter nodes for each criteria " +
						"parameter in the SQM tree as part of the QueryParameter -> SqmParameter -> JdbcParameter " +
						"transformation.  Each occurrence requires a unique SqmParameter to make sure we" +
						"ultimately get the complete set of JdbcParameter references"
		);
	}

	@Override
	public void visitSubSelectableNodes(Consumer<SqmSelectableNode<?>> jpaSelectionConsumer) {
		// nothing to do
	}

//	@Override
//	public Expression toSqlExpression(
//			Clause clause,
//			SqmToSqlAstConverter walker,
//			SqlAstCreationState sqlAstCreationState) {
//
//		final MappingModelExpressable mappingModelExpressable = DomainModelHelper.resolveMappingModelExpressable(
//				jpaCriteriaParameter,
//				sqlAstCreationState
//		);
//
//		final List<JdbcMapping> jdbcMappings = mappingModelExpressable.getJdbcMappings(
//				sqlAstCreationState.getCreationContext().getDomainModel().getTypeConfiguration()
//		);
//
//		if ( jdbcMappings.size() == 1 ) {
//			return new JdbcParameterImpl( jdbcMappings.get( 0 ) );
//		}
//
//		final SqlTuple.Builder tupleBuilder = new SqlTuple.Builder( mappingModelExpressable );
//		for ( JdbcMapping jdbcMapping : jdbcMappings ) {
//			tupleBuilder.addSubExpression( new JdbcParameterImpl( jdbcMapping ) );
//		}
//		return tupleBuilder.buildTuple();
//	}
}
