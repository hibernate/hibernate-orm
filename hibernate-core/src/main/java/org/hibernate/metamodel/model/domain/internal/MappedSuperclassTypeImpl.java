/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassTypeDescriptor;

/**
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeImpl<X> extends AbstractIdentifiableType<X> implements MappedSuperclassTypeDescriptor<X> {
	public MappedSuperclassTypeImpl(
			Class<X> javaType,
			MappedSuperclass mappedSuperclass,
			IdentifiableTypeDescriptor<? super X> superType,
			SessionFactoryImplementor sessionFactory) {
		super(
				javaType,
				javaType.getName(),
				superType,
				mappedSuperclass.getDeclaredIdentifierMapper() != null || ( superType != null && superType.hasIdClass() ),
				mappedSuperclass.hasIdentifierProperty(),
				mappedSuperclass.isVersioned(),
				sessionFactory
		);
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.MAPPED_SUPERCLASS;
	}

	@Override
	public <S extends X> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		throw new NotYetImplementedException(  );
	}
}
