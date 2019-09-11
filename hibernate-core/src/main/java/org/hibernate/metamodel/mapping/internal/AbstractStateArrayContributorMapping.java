/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.StateArrayContributorMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractStateArrayContributorMapping
		extends AbstractAttributeMapping
		implements StateArrayContributorMapping {

	private final StateArrayContributorMetadataAccess attributeMetadataAccess;
	private final int stateArrayPosition;
	private final FetchStrategy mappedFetchStrategy;


	public AbstractStateArrayContributorMapping(
			String name,
			MappingType type,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			int stateArrayPosition,
			ManagedMappingType declaringType) {
		super( name, type, declaringType );
		this.attributeMetadataAccess = attributeMetadataAccess;
		this.mappedFetchStrategy = mappedFetchStrategy;
		this.stateArrayPosition = stateArrayPosition;
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public StateArrayContributorMetadataAccess getAttributeMetadataAccess() {
		return attributeMetadataAccess;
	}

	@Override
	public String getFetchableName() {
		return getAttributeName();
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return mappedFetchStrategy;
	}
}
