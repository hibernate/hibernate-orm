/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.mapping.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.mapping.spi.Navigable;
import org.hibernate.metamodel.model.mapping.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmSpecificPluralPartPath<T> extends AbstractSqmPath<T> implements SqmPath<T> {
	private final NavigablePath navigablePath;
	private final SqmPath pluralDomainPath;
	private final PersistentCollectionDescriptor<?,?,T> collectionDescriptor;

	private String alias;

	public AbstractSqmSpecificPluralPartPath(
			NavigablePath navigablePath,
			SqmPath<?> pluralDomainPath,
			Navigable<T> referencedNavigable) {
		super(
				navigablePath,
				referencedNavigable,
				pluralDomainPath,
				pluralDomainPath.nodeBuilder()
		);
		this.navigablePath = navigablePath;
		this.pluralDomainPath = pluralDomainPath;

		//noinspection unchecked
		this.collectionDescriptor = pluralDomainPath.sqmAs( PersistentCollectionDescriptor.class );
	}

	public SqmPath getPluralDomainPath() {
		return pluralDomainPath;
	}

	public PersistentCollectionDescriptor<?,?,T> getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public SqmPath getLhs() {
		return pluralDomainPath;
	}

	@Override
	public String getExplicitAlias() {
		return alias;
	}

	@Override
	public void setExplicitAlias(String explicitAlias) {
		this.alias = explicitAlias;
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		if ( getReferencedPathSource() instanceof EntityValuedNavigable ) {
			throw new NotYetImplementedFor6Exception();
		}

		throw new UnsupportedOperationException(  );
	}
}
