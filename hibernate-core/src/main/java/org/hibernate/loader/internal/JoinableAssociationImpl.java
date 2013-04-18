/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.internal;
import java.util.Map;

import org.hibernate.Filter;
import org.hibernate.MappingException;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.spi.CollectionFetch;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.EntityFetch;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.spi.JoinableAssociation;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;

/**
 * Part of the Hibernate SQL rendering internals.  This class represents
 * a joinable association.
 *
 * @author Gavin King
 * @author Gail Badner
 */
public class JoinableAssociationImpl implements JoinableAssociation {
	private final PropertyPath propertyPath;
	private final Joinable joinable;
	private final AssociationType joinableType;
	private final String[] rhsColumns;
	private final Fetch currentFetch;
	private final EntityReference currentEntityReference;
	private final CollectionReference currentCollectionReference;
	private final JoinType joinType;
	private final String withClause;
	private final Map<String, Filter> enabledFilters;
	private final boolean hasRestriction;

	public JoinableAssociationImpl(
			EntityFetch entityFetch,
			CollectionReference currentCollectionReference,
			String withClause,
			boolean hasRestriction,
			SessionFactoryImplementor factory,
			Map<String, Filter> enabledFilters) throws MappingException {
		this(
				entityFetch,
				entityFetch.getAssociationType(),
				entityFetch,
				currentCollectionReference,
				withClause,
				hasRestriction,
				factory,
				enabledFilters
		);
	}

	public JoinableAssociationImpl(
			CollectionFetch collectionFetch,
			EntityReference currentEntityReference,
			String withClause,
			boolean hasRestriction,
			SessionFactoryImplementor factory,
			Map<String, Filter> enabledFilters) throws MappingException {
		this(
				collectionFetch,
				collectionFetch.getCollectionPersister().getCollectionType(),
				currentEntityReference,
				collectionFetch,
				withClause,
				hasRestriction,
				factory,
				enabledFilters
		);
	}

	private JoinableAssociationImpl(
			Fetch currentFetch,
			AssociationType associationType,
			EntityReference currentEntityReference,
			CollectionReference currentCollectionReference,
			String withClause,
			boolean hasRestriction,
			SessionFactoryImplementor factory,
			Map<String, Filter> enabledFilters) throws MappingException {
		this.propertyPath = currentFetch.getPropertyPath();
		this.joinableType = associationType;
		final OuterJoinLoadable ownerPersister = (OuterJoinLoadable) currentFetch.getOwner().retrieveFetchSourcePersister();
		final int propertyNumber = ownerPersister.getEntityMetamodel().getPropertyIndex( currentFetch.getOwnerPropertyName() );
		final boolean isNullable = ownerPersister.isSubclassPropertyNullable( propertyNumber );
		if ( currentFetch.getFetchStrategy().getStyle() == FetchStyle.JOIN ) {
			joinType = isNullable ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN;
		}
		else {
			joinType = JoinType.NONE;
		}
		this.joinable = joinableType.getAssociatedJoinable(factory);
		this.rhsColumns = JoinHelper.getRHSColumnNames( joinableType, factory );
		this.currentFetch = currentFetch;
		this.currentEntityReference = currentEntityReference;
		this.currentCollectionReference = currentCollectionReference;
		this.withClause = withClause;
		this.hasRestriction = hasRestriction;
		this.enabledFilters = enabledFilters; // needed later for many-to-many/filter application
	}


	/*
					public JoinableAssociationImpl(
						EntityFetch entityFetch,
						String currentCollectionSuffix,
						String withClause,
						boolean hasRestriction,
						SessionFactoryImplementor factory,
						Map enabledFilters,
						LoadQueryAliasResolutionContext aliasResolutionContext) throws MappingException {
					this.propertyPath = entityFetch.getPropertyPath();
					this.joinableType = entityFetch.getAssociationType();
					// TODO: this is not correct
					final EntityPersister fetchSourcePersister = entityFetch.getOwner().retrieveFetchSourcePersister();
					final int propertyNumber = fetchSourcePersister.getEntityMetamodel().getPropertyIndex( entityFetch.getOwnerPropertyName() );

					if ( EntityReference.class.isInstance( entityFetch.getOwner() ) ) {
						this.lhsAlias = aliasResolutionContext.resolveEntitySqlTableAlias( (EntityReference) entityFetch.getOwner() );
					}
					else {
						throw new NotYetImplementedException( "Cannot determine LHS alias for a FetchOwner that is not an EntityReference." );
					}
					final OuterJoinLoadable ownerPersister = (OuterJoinLoadable) entityFetch.getOwner().retrieveFetchSourcePersister();
					this.lhsColumns = JoinHelper.getAliasedLHSColumnNames(
							entityFetch.getAssociationType(), lhsAlias, propertyNumber, ownerPersister, factory
					);
					this.rhsAlias = aliasResolutionContext.resolveEntitySqlTableAlias( entityFetch );

					final boolean isNullable = ownerPersister.isSubclassPropertyNullable( propertyNumber );
					if ( entityFetch.getFetchStrategy().getStyle() == FetchStyle.JOIN ) {
						joinType = isNullable ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN;
					}
					else {
						joinType = JoinType.NONE;
					}
					this.joinable = joinableType.getAssociatedJoinable(factory);
					this.rhsColumns = JoinHelper.getRHSColumnNames( joinableType, factory );
					this.currentEntitySuffix = 	aliasResolutionContext.resolveEntityColumnAliases( entityFetch ).getSuffix();
					this.currentCollectionSuffix = currentCollectionSuffix;
					this.on = joinableType.getOnCondition( rhsAlias, factory, enabledFilters )
							+ ( withClause == null || withClause.trim().length() == 0 ? "" : " and ( " + withClause + " )" );
					this.hasRestriction = hasRestriction;
					this.enabledFilters = enabledFilters; // needed later for many-to-many/filter application
				}

				public JoinableAssociationImpl(
						CollectionFetch collectionFetch,
						String currentEntitySuffix,
						String withClause,
						boolean hasRestriction,
						SessionFactoryImplementor factory,
						Map enabledFilters,
						LoadQueryAliasResolutionContext aliasResolutionContext) throws MappingException {
					this.propertyPath = collectionFetch.getPropertyPath();
					final CollectionType collectionType =  collectionFetch.getCollectionPersister().getCollectionType();
					this.joinableType = collectionType;
					// TODO: this is not correct
					final EntityPersister fetchSourcePersister = collectionFetch.getOwner().retrieveFetchSourcePersister();
					final int propertyNumber = fetchSourcePersister.getEntityMetamodel().getPropertyIndex( collectionFetch.getOwnerPropertyName() );

					if ( EntityReference.class.isInstance( collectionFetch.getOwner() ) ) {
						this.lhsAlias = aliasResolutionContext.resolveEntitySqlTableAlias( (EntityReference) collectionFetch.getOwner() );
					}
					else {
						throw new NotYetImplementedException( "Cannot determine LHS alias for a FetchOwner that is not an EntityReference." );
					}
					final OuterJoinLoadable ownerPersister = (OuterJoinLoadable) collectionFetch.getOwner().retrieveFetchSourcePersister();
					this.lhsColumns = JoinHelper.getAliasedLHSColumnNames(
							collectionType, lhsAlias, propertyNumber, ownerPersister, factory
					);
					this.rhsAlias = aliasResolutionContext.resolveCollectionSqlTableAlias( collectionFetch );

					final boolean isNullable = ownerPersister.isSubclassPropertyNullable( propertyNumber );
					if ( collectionFetch.getFetchStrategy().getStyle() == FetchStyle.JOIN ) {
						joinType = isNullable ? JoinType.LEFT_OUTER_JOIN : JoinType.INNER_JOIN;
					}
					else {
						joinType = JoinType.NONE;
					}
					this.joinable = joinableType.getAssociatedJoinable(factory);
					this.rhsColumns = JoinHelper.getRHSColumnNames( joinableType, factory );
					this.currentEntitySuffix = currentEntitySuffix;
					this.currentCollectionSuffix = aliasResolutionContext.resolveCollectionColumnAliases( collectionFetch ).getSuffix();
					this.on = joinableType.getOnCondition( rhsAlias, factory, enabledFilters )
							+ ( withClause == null || withClause.trim().length() == 0 ? "" : " and ( " + withClause + " )" );
					this.hasRestriction = hasRestriction;
					this.enabledFilters = enabledFilters; // needed later for many-to-many/filter application
				}
				 */

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public JoinType getJoinType() {
		return joinType;
	}

	@Override
	public Fetch getCurrentFetch() {
		return currentFetch;
	}

	@Override
	public EntityReference getCurrentEntityReference() {
		return currentEntityReference;
	}

	@Override
	public CollectionReference getCurrentCollectionReference() {
		return currentCollectionReference;
	}

	private boolean isOneToOne() {
		if ( joinableType.isEntityType() )  {
			EntityType etype = (EntityType) joinableType;
			return etype.isOneToOne() /*&& etype.isReferenceToPrimaryKey()*/;
		}
		else {
			return false;
		}
	}

	@Override
	public AssociationType getJoinableType() {
		return joinableType;
	}

	@Override
	public boolean isCollection() {
		return getJoinableType().isCollectionType();
	}

	@Override
	public Joinable getJoinable() {
		return joinable;
	}

	public boolean hasRestriction() {
		return hasRestriction;
	}

	@Override
	public boolean isManyToManyWith(JoinableAssociation other) {
		if ( joinable.isCollection() ) {
			QueryableCollection persister = ( QueryableCollection ) joinable;
			if ( persister.isManyToMany() ) {
				return persister.getElementType() == other.getJoinableType();
			}
		}
		return false;
	}

	@Override
	public String[] getRhsColumns() {
		return rhsColumns;
	}

	@Override
	public String getWithClause() {
		return withClause;
	}

	@Override
	public Map<String, Filter> getEnabledFilters() {
		return enabledFilters;
	}
}
