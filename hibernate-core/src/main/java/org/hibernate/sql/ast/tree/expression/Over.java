/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.List;

import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.query.common.FrameExclusion;
import org.hibernate.query.common.FrameKind;
import org.hibernate.query.common.FrameMode;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.select.SortSpecification;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * @author Christian Beikov
 */
public class Over<T> implements Expression, DomainResultProducer<T> {

	private final Expression expression;
	private final List<Expression> partitions;
	private final List<SortSpecification> orderList;
	private final FrameMode mode;
	private final FrameKind startKind;
	private final Expression startExpression;
	private final FrameKind endKind;
	private final Expression endExpression;
	private final FrameExclusion exclusion;

	public Over(
			Expression expression,
			List<Expression> partitions,
			List<SortSpecification> orderList) {
		this.expression = expression;
		this.partitions = partitions;
		this.orderList = orderList;
		this.mode = FrameMode.RANGE;
		this.startKind = FrameKind.UNBOUNDED_PRECEDING;
		this.startExpression = null;
		this.endKind = FrameKind.CURRENT_ROW;
		this.endExpression = null;
		this.exclusion = FrameExclusion.NO_OTHERS;
	}

	public Over(
			Expression expression,
			List<Expression> partitions,
			List<SortSpecification> orderList,
			FrameMode mode,
			FrameKind startKind,
			Expression startExpression,
			FrameKind endKind,
			Expression endExpression,
			FrameExclusion exclusion) {
		this.expression = expression;
		this.partitions = partitions;
		this.orderList = orderList;
		this.mode = mode;
		this.startKind = startKind;
		this.startExpression = startExpression;
		this.endKind = endKind;
		this.endExpression = endExpression;
		this.exclusion = exclusion;
	}

	public Expression getExpression() {
		return expression;
	}

	public List<Expression> getPartitions() {
		return partitions;
	}

	public List<SortSpecification> getOrderList() {
		return orderList;
	}

	public FrameMode getMode() {
		return mode;
	}

	public FrameKind getStartKind() {
		return startKind;
	}

	public Expression getStartExpression() {
		return startExpression;
	}

	public FrameKind getEndKind() {
		return endKind;
	}

	public Expression getEndExpression() {
		return endExpression;
	}

	public FrameExclusion getExclusion() {
		return exclusion;
	}

	@Override
	public JdbcMappingContainer getExpressionType() {
		return expression.getExpressionType();
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitOver( this );
	}

	@Override
	public DomainResult<T> createDomainResult(String resultVariable, DomainResultCreationState creationState) {
		final SqlSelection sqlSelection = createSelection( creationState.getSqlAstCreationState() );
		return new BasicResult<>(
				sqlSelection.getValuesArrayPosition(),
				resultVariable,
				expression.getExpressionType().getSingleJdbcMapping()
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		createSelection( creationState.getSqlAstCreationState() );
	}

	private SqlSelection createSelection(SqlAstCreationState creationState) {
		return creationState.getSqlExpressionResolver().resolveSqlSelection(
				this,
				expression.getExpressionType().getSingleJdbcMapping().getJdbcJavaType(),
				null,
				creationState.getCreationContext().getSessionFactory().getTypeConfiguration()
		);
	}

}
