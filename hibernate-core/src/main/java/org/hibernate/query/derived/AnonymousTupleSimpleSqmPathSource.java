/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.derived;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.internal.PathHelper;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleSimpleSqmPathSource<J> implements SqmPathSource<J> {
	private final String localPathName;
	private final DomainType<J> domainType;
	private final BindableType jpaBindableType;

	public AnonymousTupleSimpleSqmPathSource(
			String localPathName,
			DomainType<J> domainType,
			BindableType jpaBindableType) {
		this.localPathName = localPathName;
		this.domainType = domainType;
		this.jpaBindableType = jpaBindableType;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return domainType.getBindableJavaType();
	}

	@Override
	public String getPathName() {
		return localPathName;
	}

	@Override
	public DomainType<J> getSqmPathType() {
		return domainType;
	}

	@Override
	public BindableType getBindableType() {
		return jpaBindableType;
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return domainType.getExpressibleJavaType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new IllegalStateException( "Basic paths cannot be dereferenced" );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		return new SqmBasicValuedSimplePath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				this,
				lhs,
				lhs.nodeBuilder()
		);
	}
}
