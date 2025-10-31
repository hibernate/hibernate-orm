/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.spi.NavigablePath;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmSpecificPluralPartPath<T> extends AbstractSqmPath<T> implements SqmPath<T> {
	private final SqmPluralValuedSimplePath<?> pluralDomainPath;
	private final PluralPersistentAttribute<?, ?, ?> pluralAttribute;

	public AbstractSqmSpecificPluralPartPath(
			NavigablePath navigablePath,
			SqmPluralValuedSimplePath<?> pluralDomainPath,
			PluralPersistentAttribute<?, ?, ?> referencedAttribute,
			SqmPathSource<T> pathSource) {
		super(
				navigablePath,
				pathSource,
				pluralDomainPath,
				pluralDomainPath.nodeBuilder()
		);
		this.pluralDomainPath = pluralDomainPath;
		this.pluralAttribute = referencedAttribute;
	}

	public SqmPluralValuedSimplePath<?> getPluralDomainPath() {
		return pluralDomainPath;
	}

	public PluralPersistentAttribute<?, ?, ?> getPluralAttribute() {
		return pluralAttribute;
	}

	@Override
	public @NonNull SqmPath<?> getLhs() {
		return pluralDomainPath;
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType, @Nullable String alias) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget, @Nullable String alias, boolean fetch) {
		throw new UnsupportedOperationException(  );
	}
}
