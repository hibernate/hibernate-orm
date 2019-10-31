/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityCollectionPart implements CollectionPart, EntityValuedModelPart {
	private final Nature nature;
	private final EntityMappingType entityMappingType;

	@SuppressWarnings("WeakerAccess")
	public EntityCollectionPart(Nature nature, EntityMappingType entityMappingType) {
		this.nature = nature;
		this.entityMappingType = entityMappingType;
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public EntityMappingType getPartTypeDescriptor() {
		return getEntityMappingType();
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return entityMappingType;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getEntityMappingType().getJavaTypeDescriptor();
	}

	@Override
	public String getFetchableName() {
		return nature.getName();
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return FetchStrategy.IMMEDIATE_JOIN;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public int getNumberOfFetchables() {
		return entityMappingType.getNumberOfFetchables();
	}
}
