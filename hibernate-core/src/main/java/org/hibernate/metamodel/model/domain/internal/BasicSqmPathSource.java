/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.TerminalPathException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class BasicSqmPathSource<J>
		extends AbstractSqmPathSource<J>
		implements ReturnableType<J> {
	private final JavaType<?> relationalJavaType;
	private final boolean isGeneric;

	public BasicSqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			BasicDomainType<J> domainType,
			JavaType<?> relationalJavaType,
			BindableType jpaBindableType,
			boolean isGeneric) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		this.relationalJavaType = relationalJavaType;
		this.isGeneric = isGeneric;
	}

	@Override
	public BasicDomainType<J> getSqmPathType() {
		return (BasicDomainType<J>) super.getSqmPathType();
	}

	@Override
	public DomainType<J> getSqmType() {
		return getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		String path = pathModel.getPathName();
		String pathDesc = path==null || path.startsWith("{") ? " " : " '" + pathModel.getPathName() + "' ";
		throw new TerminalPathException( "Terminal path" + pathDesc + "has no attribute '" + name + "'" );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		return new SqmBasicValuedSimplePath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return relationalJavaType;
	}

	@Override
	public boolean isGeneric() {
		return isGeneric;
	}

	@Override
	public String toString() {
		return "BasicSqmPathSource(" +
				getPathName() + " : " + getJavaType().getSimpleName() +
				")";
	}
}
