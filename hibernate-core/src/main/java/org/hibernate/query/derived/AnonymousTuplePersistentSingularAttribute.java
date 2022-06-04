/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.lang.reflect.Member;

import org.hibernate.Incubating;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTuplePersistentSingularAttribute<O, J> extends AnonymousTupleSqmPathSource<J> implements
		SingularPersistentAttribute<O, J> {

	private final SingularPersistentAttribute<O, J> delegate;

	public AnonymousTuplePersistentSingularAttribute(
			String localPathName,
			SqmPath<J> path,
			SingularPersistentAttribute<O, J> delegate) {
		super( localPathName, path );
		this.delegate = delegate;
	}

	@Override
	public SqmJoin createSqmJoin(
			SqmFrom lhs,
			SqmJoinType joinType,
			String alias,
			boolean fetched,
			SqmCreationState creationState) {
		return new SqmSingularJoin<>(
				lhs,
				this,
				alias,
				joinType,
				fetched,
				creationState.getCreationContext().getNodeBuilder()
		);
	}

	@Override
	public SimpleDomainType<J> getType() {
		return delegate.getType();
	}

	@Override
	public ManagedDomainType<O> getDeclaringType() {
		return delegate.getDeclaringType();
	}

	@Override
	public boolean isId() {
		return delegate.isId();
	}

	@Override
	public boolean isVersion() {
		return delegate.isVersion();
	}

	@Override
	public boolean isOptional() {
		return delegate.isOptional();
	}

	@Override
	public JavaType<J> getAttributeJavaType() {
		return delegate.getAttributeJavaType();
	}

	@Override
	public AttributeClassification getAttributeClassification() {
		return delegate.getAttributeClassification();
	}

	@Override
	public SimpleDomainType<?> getKeyGraphType() {
		return delegate.getKeyGraphType();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return delegate.getPersistentAttributeType();
	}

	@Override
	public Member getJavaMember() {
		return delegate.getJavaMember();
	}

	@Override
	public boolean isAssociation() {
		return delegate.isAssociation();
	}

	@Override
	public boolean isCollection() {
		return delegate.isCollection();
	}
}
