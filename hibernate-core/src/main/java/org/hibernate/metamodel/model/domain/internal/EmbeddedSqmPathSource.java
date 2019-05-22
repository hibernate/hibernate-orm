/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class EmbeddedSqmPathSource<J> extends AbstractSqmPathSource<J> {
	public EmbeddedSqmPathSource(
			String localPathName,
			EmbeddableDomainType<J> domainType,
			BindableType jpaBindableType,
			NodeBuilder nodeBuilder) {
		super( localPathName, domainType, jpaBindableType, nodeBuilder );
	}

	@Override
	public EmbeddableDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (EmbeddableDomainType<J>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return (SqmPathSource<?>) getSqmPathType().findAttribute( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmCreationState creationState) {
		return new SqmAnyValuedSimplePath<>(
				lhs.getNavigablePath().append( getPathName() ),
				this,
				lhs,
				getNodeBuilder()
		);
	}
}
