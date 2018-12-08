/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.metamodel.Bindable;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeBasic;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public class SingularPathBasic<T> extends AbstractPath<T> implements SingularPath<T> {
	public SingularPathBasic(
			PathSourceImplementor pathSource,
			SingularPersistentAttributeBasic<?,T> attribute,
			CriteriaNodeBuilder criteriaBuilder) {
		super( attribute, pathSource, criteriaBuilder );
	}

	@Override
	@SuppressWarnings("unchecked")
	public SingularPersistentAttributeBasic<?,T> getNavigable() {
		return (SingularPersistentAttributeBasic) super.getNavigable();
	}

	@Override
	public PathSourceImplementor<T> asPathSource(String subPathName) throws PathException {
		throw illegalDereference( subPathName );
	}

	@Override
	public Bindable<T> getModel() {
		return getNavigable();
	}

	@Override
	public <S extends T> PathImplementor<S> treatAs(Class<S> treatJavaType) throws PathException {
		throw notTreatable();
	}
}
