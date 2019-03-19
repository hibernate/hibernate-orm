/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import java.util.function.Supplier;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.TrimSpecification;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Needed to pass TrimSpecification as an SqmExpression when we call out to
 * SqmFunctionTemplates handling TRIM calls as a function argument.
 *
 * @author Steve Ebersole
 */
public class TrimSpecificationExpressionWrapper implements SqmExpression {
	private static final TrimSpecificationExpressionWrapper LEADING = new TrimSpecificationExpressionWrapper( TrimSpecification.LEADING );
	private static final TrimSpecificationExpressionWrapper TRAILING = new TrimSpecificationExpressionWrapper( TrimSpecification.TRAILING );
	private static final TrimSpecificationExpressionWrapper BOTH = new TrimSpecificationExpressionWrapper( TrimSpecification.BOTH );

	private final TrimSpecification specification;

	private TrimSpecificationExpressionWrapper(TrimSpecification specification) {
		this.specification = specification;
	}

	public TrimSpecification getSpecification() {
		return specification;
	}

	@Override
	public ExpressableType getExpressableType() {
		return null;
	}

	@Override
	public Supplier<ExpressableType> getInferableType() {
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
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
}
