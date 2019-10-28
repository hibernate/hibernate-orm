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
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeMappingImpl extends AbstractAttributeMapping implements PluralAttributeMapping {
	private final int stateArrayPosition;
	private final PropertyAccess propertyAccess;
	private final StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess;

	private final FetchStrategy fetchStrategy;

	private final String tableExpression;
	private final String[] attrColumnExpressions;

	private final ModelPart elementDescriptor;
	private final ModelPart indexDescriptor;

	private final CascadeStyle cascadeStyle;
	private final CollectionPersister collectionDescriptor;

	@SuppressWarnings("WeakerAccess")
	public PluralAttributeMappingImpl(
			String attributeName,
			PropertyAccess propertyAccess,
			StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess,
			CollectionMappingType collectionMappingType,
			int stateArrayPosition,
			String tableExpression,
			String[] attrColumnExpressions,
			ModelPart elementDescriptor,
			ModelPart indexDescriptor,
			FetchStrategy fetchStrategy,
			CascadeStyle cascadeStyle,
			ManagedMappingType declaringType,
			CollectionPersister collectionDescriptor) {
		super( attributeName, collectionMappingType, declaringType );
		this.propertyAccess = propertyAccess;
		this.stateArrayContributorMetadataAccess = stateArrayContributorMetadataAccess;
		this.stateArrayPosition = stateArrayPosition;
		this.tableExpression = tableExpression;
		this.attrColumnExpressions = attrColumnExpressions;
		this.elementDescriptor = elementDescriptor;
		this.indexDescriptor = indexDescriptor;
		this.fetchStrategy = fetchStrategy;
		this.cascadeStyle = cascadeStyle;
		this.collectionDescriptor = collectionDescriptor;
	}


	@Override
	public CollectionMappingType getMappedTypeDescriptor() {
		return (CollectionMappingType) super.getMappedTypeDescriptor();
	}

	@Override
	public CollectionPersister getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public ModelPart getValueDescriptor() {
		return elementDescriptor;
	}

	@Override
	public ModelPart getIndexDescriptor() {
		return indexDescriptor;
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public StateArrayContributorMetadataAccess getAttributeMetadataAccess() {
		return stateArrayContributorMetadataAccess;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public String getFetchableName() {
		return getAttributeName();
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return fetchStrategy;
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
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup collectionTableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup( fetchablePath );

		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
