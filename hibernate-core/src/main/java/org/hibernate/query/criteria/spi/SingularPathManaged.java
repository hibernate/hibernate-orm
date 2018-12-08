/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria.spi;

import javax.persistence.metamodel.Bindable;

import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.criteria.spi.AbstractPath;
import org.hibernate.query.criteria.spi.CriteriaNodeBuilder;
import org.hibernate.query.criteria.spi.PathImplementor;
import org.hibernate.query.criteria.spi.PathSourceImplementor;
import org.hibernate.query.criteria.spi.SingularPath;
import org.hibernate.query.criteria.spi.TreatedPath;

/**
 * @author Steve Ebersole
 */
public class SingularPathManaged<T> extends AbstractPath<T> implements SingularPath<T>, PathSourceImplementor<T> {
	public SingularPathManaged(
			PathSourceImplementor pathSource,
			SingularPersistentAttribute<?, T> attribute,
			CriteriaNodeBuilder nodeBuilder) {
		super( attribute, pathSource, nodeBuilder );
	}

	public SingularPersistentAttribute<?,T> getAttribute() {
		return getNavigable();
	}

	@Override
	public SingularPersistentAttribute<?,T> getNavigable() {
		return getAttribute();
	}

	@Override
	public PathSourceImplementor<T> asPathSource(String subPathName) throws PathException {
		return this;
	}

	@Override
	public Bindable<T> getModel() {
		return getAttribute();
	}

	@Override
	@SuppressWarnings("unchecked")
	public PathImplementor treatAs(Class treatJavaType) throws PathException {
		return new TreatedPath(
				this,
				nodeBuilder().getSessionFactory().getMetamodel().entity( treatJavaType ),
				nodeBuilder()
		);
	}

	@Override
	@SuppressWarnings("unchecked")
	public ManagedTypeDescriptor<T> getManagedType() {
		return (ManagedTypeDescriptor<T>) getAttribute().getAttributeType();
	}
}
