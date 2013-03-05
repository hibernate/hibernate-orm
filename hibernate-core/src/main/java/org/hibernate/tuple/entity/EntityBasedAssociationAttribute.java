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
package org.hibernate.tuple.entity;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.walking.internal.Helper;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.AssociationType;
import org.hibernate.type.ForeignKeyDirection;

import static org.hibernate.engine.internal.JoinHelper.getLHSColumnNames;
import static org.hibernate.engine.internal.JoinHelper.getLHSTableName;
import static org.hibernate.engine.internal.JoinHelper.getRHSColumnNames;

/**
* @author Steve Ebersole
*/
public class EntityBasedAssociationAttribute
		extends AbstractEntityBasedAttribute
		implements AssociationAttributeDefinition {

	private Joinable joinable;

	public EntityBasedAssociationAttribute(
			EntityPersister source,
			SessionFactoryImplementor sessionFactory,
			int attributeNumber,
			String attributeName,
			AssociationType attributeType,
			BaselineAttributeInformation baselineInfo) {
		super( source, sessionFactory, attributeNumber, attributeName, attributeType, baselineInfo );
	}

	@Override
	public AssociationType getType() {
		return (AssociationType) super.getType();
	}

	protected Joinable getJoinable() {
		if ( joinable == null ) {
			joinable = getType().getAssociatedJoinable( sessionFactory() );
		}
		return joinable;
	}

	@Override
	public AssociationKey getAssociationKey() {
		final AssociationType type = getType();
		final Joinable joinable = type.getAssociatedJoinable( sessionFactory() );

		if ( type.getForeignKeyDirection() == ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT ) {
			final String lhsTableName;
			final String[] lhsColumnNames;

			if ( joinable.isCollection() ) {
				final QueryableCollection collectionPersister = (QueryableCollection) joinable;
				lhsTableName = collectionPersister.getTableName();
				lhsColumnNames = collectionPersister.getElementColumnNames();
			}
			else {
				final OuterJoinLoadable entityPersister = (OuterJoinLoadable) joinable;
				lhsTableName = getLHSTableName( type, attributeNumber(), entityPersister );
				lhsColumnNames = getLHSColumnNames( type, attributeNumber(), entityPersister, sessionFactory() );
			}
			return new AssociationKey( lhsTableName, lhsColumnNames );
		}
		else {
			return new AssociationKey( joinable.getTableName(), getRHSColumnNames( type, sessionFactory() ) );
		}
	}

	@Override
	public boolean isCollection() {
		return getJoinable().isCollection();
	}

	@Override
	public EntityDefinition toEntityDefinition() {
		if ( isCollection() ) {
			throw new IllegalStateException( "Cannot treat collection-valued attribute as entity type" );
		}
		return (EntityPersister) getJoinable();
	}

	@Override
	public CollectionDefinition toCollectionDefinition() {
		if ( ! isCollection() ) {
			throw new IllegalStateException( "Cannot treat entity-valued attribute as collection type" );
		}
		return (QueryableCollection) getJoinable();
	}

	@Override
	public FetchStrategy determineFetchPlan(LoadQueryInfluencers loadQueryInfluencers, PropertyPath propertyPath) {
		final EntityPersister owningPersister = getSource().getEntityPersister();

		FetchStyle style = Helper.determineFetchStyleByProfile(
				loadQueryInfluencers,
				owningPersister,
				propertyPath,
				attributeNumber()
		);
		if ( style == null ) {
			style = Helper.determineFetchStyleByMetadata(
					((OuterJoinLoadable) getSource().getEntityPersister()).getFetchMode( attributeNumber() ),
					getType(),
					sessionFactory()
			);
		}

		return new FetchStrategy(
				Helper.determineFetchTiming( style, getType(), sessionFactory() ),
				style
		);
	}

	@Override
	public CascadeStyle determineCascadeStyle() {
		return getSource().getEntityPersister().getPropertyCascadeStyles()[attributeNumber()];
	}
}
