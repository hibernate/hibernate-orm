/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import javax.persistence.criteria.Expression;

import org.hibernate.QueryException;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.TrimSpecification;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Needed to pass TrimSpecification as an SqmExpression when we call out to
 * SqmFunctionTemplates handling TRIM calls as a function argument.
 *
 * @author Steve Ebersole
 */
public class TrimSpecificationExpressionWrapper<T> implements SqmExpression<T> {
	private static final TrimSpecificationExpressionWrapper LEADING = new TrimSpecificationExpressionWrapper( TrimSpecification.LEADING );
	private static final TrimSpecificationExpressionWrapper TRAILING = new TrimSpecificationExpressionWrapper( TrimSpecification.TRAILING );
	private static final TrimSpecificationExpressionWrapper BOTH = new TrimSpecificationExpressionWrapper( TrimSpecification.BOTH );

	private final TrimSpecification specification;

	public static TrimSpecificationExpressionWrapper from(TrimSpecification specification) {
		if ( specification == null ) {
			return null;
		}
		switch ( specification ) {
			case TRAILING: return TRAILING;
			case LEADING: return LEADING;
			case BOTH: return BOTH;
		}
		throw new UnsupportedOperationException();
	}

	private TrimSpecificationExpressionWrapper(TrimSpecification specification) {
		this.specification = specification;
	}

	public TrimSpecification getSpecification() {
		return specification;
	}

	@Override
	public ExpressableType<T> getExpressableType() {
		return null;
	}

	@Override
	public void applyInferableType(ExpressableType<?> type) {
		// nothing to do
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return null;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String asLoggableText() {
		return specification.name();
	}

	public static TrimSpecificationExpressionWrapper wrap(TrimSpecification specification) {
		switch ( specification ) {
			case LEADING: return LEADING;
			case TRAILING: return TRAILING;
			default: return BOTH;
		}
	}

	@Override
	public SqmExpression<Long> asLong() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmExpression<Integer> asInteger() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmExpression<Float> asFloat() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmExpression<Double> asDouble() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmExpression<BigDecimal> asBigDecimal() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmExpression<BigInteger> asBigInteger() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmExpression<String> asString() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <X> SqmExpression<X> as(Class<X> type) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPredicate isNull() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPredicate isNotNull() {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPredicate in(Object... values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPredicate in(Expression<?>... values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPredicate in(Collection<?> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SqmPredicate in(Expression<Collection<?>> values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<? extends JpaSelection<?>> getSelectionItems() {
		throw new UnsupportedOperationException();
	}

	@Override
	public JpaSelection<T> alias(String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isCompoundSelection() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getAlias() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NodeBuilder nodeBuilder() {
		throw new UnsupportedOperationException();
	}
}
