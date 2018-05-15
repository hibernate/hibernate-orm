/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.results.spi.CollectionMappingNode;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.internal.CollectionJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionMappingNode implements CollectionMappingNode {
	private final FetchParent fetchParent;
	private final PluralPersistentAttribute pluralAttribute;
	private final String resultVariable;

	private final DomainResult keyContainerResult;
	private final DomainResult keyCollectionResult;

	@SuppressWarnings("WeakerAccess")
	protected AbstractCollectionMappingNode(
			FetchParent fetchParent,
			PluralPersistentAttribute pluralAttribute,
			String resultVariable,
			DomainResult keyContainerResult,
			DomainResult keyCollectionResult) {
		this.fetchParent = fetchParent;
		this.pluralAttribute = pluralAttribute;
		this.resultVariable = resultVariable;
		this.keyContainerResult = keyContainerResult;
		this.keyCollectionResult = keyCollectionResult;
	}

	protected FetchParent getFetchParent() {
		return fetchParent;
	}

	@SuppressWarnings("WeakerAccess")
	protected PluralPersistentAttribute getPluralAttribute() {
		return pluralAttribute;
	}

	@Override
	public DomainResult getKeyContainerResult() {
		return keyContainerResult;
	}

	@Override
	public DomainResult getKeyCollectionResult() {
		return keyCollectionResult;
	}

	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public CollectionJavaDescriptor getJavaTypeDescriptor() {
		return getPluralAttribute().getJavaTypeDescriptor();
	}

	@Override
	public PersistentCollectionDescriptor getCollectionDescriptor() {
		return getPluralAttribute().getPersistentCollectionDescriptor();
	}
}
