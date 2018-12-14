/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import java.util.List;

import org.hibernate.query.criteria.JpaCompoundSelection;

/**
 * Specialization of JpaCompoundSelection to be  able to identify
 * a dynamic instantiation
 *
 * @author Steve Ebersole
 */
public class ConstructorSelection<T> extends CompoundSelection<T> implements JpaCompoundSelection<T> {
	public ConstructorSelection(
			Class<T> javaType,
			List<? extends SelectionImplementor<?>> ctorArguments,
			CriteriaNodeBuilder criteriaBuilder) {
		super( ctorArguments, javaType, criteriaBuilder );
	}

	@Override
	public <R> R accept(CriteriaVisitor visitor) {
		return visitor.visitDynamicInstantiation( this );
	}
}
