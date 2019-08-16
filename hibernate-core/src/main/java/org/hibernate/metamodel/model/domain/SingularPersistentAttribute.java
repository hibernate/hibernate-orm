/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * Hibernate extension to the JPA {@link SingularAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface SingularPersistentAttribute<D,J>
		extends SingularAttribute<D,J>, PersistentAttribute<D,J>, ModelPart, SqmPathSource<J>, SqmJoinable {
	@Override
	SimpleDomainType<J> getType();

	@Override
	ManagedDomainType<D> getDeclaringType();

	@Override
	SimpleDomainType<J> getSqmPathType();

	/**
	 * For a singular attribute, the value type is defined as the
	 * attribute type
	 */
	@Override
	default SimpleDomainType<?> getValueGraphType() {
		return getType();
	}

	@Override
	default Class<J> getJavaType() {
		return getType().getJavaType();
	}
}
