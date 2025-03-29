/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import java.lang.reflect.Member;

import org.hibernate.Incubating;
import org.hibernate.metamodel.AttributeClassification;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SqmPathSource;
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
public class AnonymousTupleSqmAssociationPathSource<O, J> extends AnonymousTupleSqmPathSource<J> implements
		SingularPersistentAttribute<O, J> {

	private final SimpleDomainType<J> domainType;

	public AnonymousTupleSqmAssociationPathSource(
			String localPathName,
			SqmPath<J> path,
			SimpleDomainType<J> domainType) {
		super( localPathName, path );
		this.domainType = domainType;
	}

	@Override
	public SqmJoin<O, J> createSqmJoin(
			SqmFrom<?, O> lhs,
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
		return domainType;
	}

	@Override
	public ManagedDomainType<O> getDeclaringType() {
		return null;
	}

	@Override
	public SqmPathSource<J> getPathSource() {
		return this;
	}

	@Override
	public boolean isId() {
		return false;
	}

	@Override
	public boolean isVersion() {
		return false;
	}

	@Override
	public boolean isOptional() {
		return true;
	}

	@Override
	public JavaType<J> getAttributeJavaType() {
		return domainType.getExpressibleJavaType();
	}

	@Override
	public AttributeClassification getAttributeClassification() {
		return AttributeClassification.MANY_TO_ONE;
	}

	@Override
	public SimpleDomainType<?> getKeyGraphType() {
		return domainType;
	}

	@Override
	public String getName() {
		return getPathName();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.MANY_TO_ONE;
	}

	@Override
	public Member getJavaMember() {
		return null;
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
