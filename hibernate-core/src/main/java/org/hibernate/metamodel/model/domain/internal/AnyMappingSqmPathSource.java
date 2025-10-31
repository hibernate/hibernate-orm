/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.internal.AnyDiscriminatorPart;
import org.hibernate.metamodel.mapping.internal.AnyKeyPart;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmAnyValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

import static jakarta.persistence.metamodel.Bindable.BindableType.SINGULAR_ATTRIBUTE;

/**
 * @author Steve Ebersole
 */
public class AnyMappingSqmPathSource<J> extends AbstractSqmPathSource<J> {
	private final SqmPathSource<?> keyPathSource;
	private final AnyDiscriminatorSqmPathSource<?> discriminatorPathSource;

	public AnyMappingSqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			AnyMappingDomainType<J> domainType,
			BindableType jpaBindableType) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		keyPathSource = new BasicSqmPathSource<>(
				AnyKeyPart.KEY_NAME,
				null,
				(BasicDomainType<?>) domainType.getKeyType(),
				domainType.getKeyType().getExpressibleJavaType(),
				SINGULAR_ATTRIBUTE,
				false
		);
		discriminatorPathSource = new AnyDiscriminatorSqmPathSource<>(
				localPathName,
				null,
				domainType.getDiscriminatorType(),
				jpaBindableType
		);
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		return switch ( name ) {
			case AnyKeyPart.KEY_NAME ->
				// standard id() function
					keyPathSource;
			case AnyDiscriminatorPart.ROLE_NAME ->
				// standard type() function
					discriminatorPathSource;
			case "id" ->
				// deprecated HQL .id syntax
					keyPathSource;
			case "class" ->
				// deprecated HQL .class syntax
					discriminatorPathSource;
			default -> throw new UnsupportedMappingException(
					"Only the key and discriminator parts of an '@Any' mapping may be dereferenced" );
		};
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		return new SqmAnyValuedSimplePath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
	}
}
