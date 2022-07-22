/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.BasicSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EmbeddedSqmPathSource;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleSqmPathSource<J> implements SqmPathSource<J> {
	private final String localPathName;
	private final SqmPath<J> path;

	public AnonymousTupleSqmPathSource(
			String localPathName,
			SqmPath<J> path) {
		this.localPathName = localPathName;
		this.path = path;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return path.getNodeJavaType().getJavaTypeClass();
	}

	@Override
	public String getPathName() {
		return localPathName;
	}

	@Override
	public DomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (DomainType<J>) path.getNodeType().getSqmPathType();
	}

	@Override
	public BindableType getBindableType() {
		return path.getNodeType().getBindableType();
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return path.getNodeJavaType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return path.getNodeType().findSubPathSource( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final NavigablePath navigablePath;
		if ( intermediatePathSource == null ) {
			navigablePath = lhs.getNavigablePath().append( getPathName() );
		}
		else {
			navigablePath = lhs.getNavigablePath().append( intermediatePathSource.getPathName() ).append( getPathName() );
		}
		final SqmPathSource<J> nodeType = path.getNodeType();
		if ( nodeType instanceof BasicSqmPathSource<?> ) {
			return new SqmBasicValuedSimplePath<>(
					navigablePath,
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}
		else if ( nodeType instanceof EmbeddedSqmPathSource<?> ) {
			return new SqmEmbeddedValuedSimplePath<>(
					navigablePath,
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}
		else if ( nodeType instanceof EntitySqmPathSource<?> || nodeType instanceof EntityDomainType<?>
				|| nodeType instanceof PersistentAttribute<?, ?> && nodeType.getSqmPathType() instanceof EntityDomainType<?> ) {
			return new SqmEntityValuedSimplePath<>(
					navigablePath,
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}

		throw new UnsupportedOperationException( "Unsupported path source: " + nodeType );
	}
}
