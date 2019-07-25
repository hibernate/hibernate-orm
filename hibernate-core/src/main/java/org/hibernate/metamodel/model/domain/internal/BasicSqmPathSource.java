/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.IllegalPathUsageException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class BasicSqmPathSource<J> extends AbstractSqmPathSource<J> implements AllowableParameterType<J> {
	@SuppressWarnings("WeakerAccess")
	public BasicSqmPathSource(
			String localPathName,
			BasicDomainType<J> domainType,
			BindableType jpaBindableType) {
		super( localPathName, domainType, jpaBindableType );
	}

	@Override
	public BasicDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (BasicDomainType<J>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new IllegalPathUsageException( "Basic paths cannot be dereferenced" );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmCreationState creationState) {
		final NavigablePath navigablePath = lhs.getNavigablePath().append( getPathName() );
		return new SqmBasicValuedSimplePath<>(
				navigablePath,
				this,
				lhs,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}
