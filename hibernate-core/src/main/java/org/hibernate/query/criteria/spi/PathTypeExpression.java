/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

/**
 * @author Steve Ebersole
 */
public class PathTypeExpression<T> extends AbstractExpression<T> {
	private final PathImplementor<T> path;

	public PathTypeExpression(
			PathImplementor<T> path,
			CriteriaNodeBuilder criteriaBuilder) {
		super( path.getJavaTypeDescriptor(), criteriaBuilder );
		this.path = path;
	}

	public PathImplementor<T> getPath() {
		return path;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <R> R accept(CriteriaVisitor visitor) {
		return (R) visitor.visitPathType( this );
	}
}
