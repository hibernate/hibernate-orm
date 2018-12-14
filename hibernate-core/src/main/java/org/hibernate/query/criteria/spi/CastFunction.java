/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Models a <tt>CAST</tt> function.
 *
 * @param <T> The cast result type.
 * @param <Y> The type of the expression to be cast.
 *
 * @author Steve Ebersole
 */
public class CastFunction<T,Y> extends AbstractStandardFunction<T> {
	public static final String CAST_NAME = "cast";

	private final ExpressionImplementor<Y> castSource;

	public CastFunction(
			ExpressionImplementor<Y> castSource,
			Class<T> javaType,
			CriteriaNodeBuilder builder) {
		super( CAST_NAME, javaType, builder );
		this.castSource = castSource;
	}

	public ExpressionImplementor<Y> getCastSource() {
		return castSource;
	}

	public JavaTypeDescriptor getCastTarget() {
		return getJavaTypeDescriptor();
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitCastFunction( this );
	}
}
