/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class EmbeddedSqmPathSource<J> extends AbstractSqmPathSource<J> implements AllowableParameterType<J> {
	public EmbeddedSqmPathSource(
			String localPathName,
			EmbeddableDomainType<J> domainType,
			BindableType jpaBindableType) {
		super( localPathName, domainType, jpaBindableType );
	}

	@Override
	public EmbeddableDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (EmbeddableDomainType<J>) super.getSqmPathType();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public Class<J> getJavaType() {
		return getBindableJavaType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return (SqmPathSource<?>) getSqmPathType().findAttribute( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmCreationState creationState) {
		return new SqmEmbeddedValuedSimplePath<>(
				lhs.getNavigablePath().append( getPathName() ),
				this,
				lhs,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}
