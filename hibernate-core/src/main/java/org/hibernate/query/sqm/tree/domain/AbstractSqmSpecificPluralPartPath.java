/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.query.NavigablePath;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmSpecificPluralPartPath implements SqmPath {
	private final NavigablePath navigablePath;
	private final SqmPath pluralDomainPath;
	private final PersistentCollectionDescriptor collectionDescriptor;

	private String alias;

	public AbstractSqmSpecificPluralPartPath(NavigablePath navigablePath, SqmPath pluralDomainPath) {
		this.navigablePath = navigablePath;
		this.pluralDomainPath = pluralDomainPath;
		this.collectionDescriptor = pluralDomainPath.getReferencedNavigable().as( PersistentCollectionDescriptor.class );
	}

	public SqmPath getPluralDomainPath() {
		return pluralDomainPath;
	}

	public PersistentCollectionDescriptor getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public String getUniqueIdentifier() {
		return null;
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
}
