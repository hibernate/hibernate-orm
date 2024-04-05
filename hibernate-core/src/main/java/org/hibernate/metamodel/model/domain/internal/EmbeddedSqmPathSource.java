/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class EmbeddedSqmPathSource<J>
		extends AbstractSqmPathSource<J>
		implements CompositeSqmPathSource<J> {
	private final boolean isGeneric;

	public EmbeddedSqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			EmbeddableDomainType<J> domainType,
			BindableType jpaBindableType,
			boolean isGeneric) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		this.isGeneric = isGeneric;
	}

	@Override
	public EmbeddableDomainType<J> getSqmPathType() {
		return (EmbeddableDomainType<J>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name, JpaMetamodelImplementor metamodel) {
		final PersistentAttribute<? super J, ?> attribute = getSqmPathType().findAttribute( name );
		if ( attribute != null ) {
			return (SqmPathSource<?>) attribute;
		}

		return (SqmPathSource<?>) getSqmPathType().findSubTypesAttribute( name );
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return (SqmPathSource<?>) getSqmPathType().findAttribute( name );
	}

	@Override
	public boolean isGeneric() {
		return isGeneric;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		return new SqmEmbeddedValuedSimplePath<>(
				pathModel instanceof SqmJoinable<?, ?>
						? ( (SqmJoinable<?, ?>) pathModel ).createNavigablePath( lhs, null )
						: PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
	}
}
