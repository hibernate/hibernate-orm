/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.PathException;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmSpecificPluralPartPath<T> extends AbstractSqmPath<T> implements SqmPath<T> {
	private final NavigablePath navigablePath;
	private final SqmPath pluralDomainPath;
	private final PluralPersistentAttribute<?,?,T> pluralAttribute;

	private String alias;

	@SuppressWarnings("WeakerAccess")
	public AbstractSqmSpecificPluralPartPath(
			NavigablePath navigablePath,
			SqmPath<?> pluralDomainPath,
			PluralPersistentAttribute<?,?,T> referencedAttribute) {
		super(
				navigablePath,
				referencedAttribute,
				pluralDomainPath,
				pluralDomainPath.nodeBuilder()
		);
		this.navigablePath = navigablePath;
		this.pluralDomainPath = pluralDomainPath;
		this.pluralAttribute = referencedAttribute;
	}

	@SuppressWarnings("WeakerAccess")
	public SqmPath getPluralDomainPath() {
		return pluralDomainPath;
	}

	@SuppressWarnings("WeakerAccess")
	public PluralPersistentAttribute<?,?,T> getPluralAttribute() {
		return pluralAttribute;
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
		if ( getReferencedPathSource().getSqmPathType() instanceof EntityDomainType ) {
			throw new NotYetImplementedFor6Exception();
		}

		throw new UnsupportedOperationException(  );
	}
}
