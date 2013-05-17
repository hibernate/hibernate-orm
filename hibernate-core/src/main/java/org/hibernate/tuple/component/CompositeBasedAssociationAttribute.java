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
package org.hibernate.tuple.component;

import org.hibernate.FetchMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.spi.HydratedCompoundValueHandler;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.tuple.BaselineAttributeInformation;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class CompositeBasedAssociationAttribute
		extends AbstractCompositeBasedAttribute
		implements AssociationAttributeDefinition {

	private final AssociationKey associationKey;
	private Joinable joinable;

	public CompositeBasedAssociationAttribute(
			AbstractCompositionAttribute source,
			SessionFactoryImplementor factory,
			int attributeNumber,
			String attributeName,
			AssociationType attributeType,
			BaselineAttributeInformation baselineInfo,
			int ownerAttributeNumber,
			AssociationKey associationKey) {
		super( source, factory, attributeNumber, attributeName, attributeType, baselineInfo, ownerAttributeNumber );
		this.associationKey = associationKey;
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
		return associationKey;
	}

	@Override
	public boolean isCollection() {
		return getJoinable().isCollection();
	}

	@Override
	public EntityDefinition toEntityDefinition() {
		if ( isCollection() ) {
			throw new IllegalStateException( "Cannot treat collection attribute as entity type" );
		}
		return (EntityPersister) getJoinable();
	}

	@Override
	public CollectionDefinition toCollectionDefinition() {
		if ( isCollection() ) {
			throw new IllegalStateException( "Cannot treat entity attribute as collection type" );
		}
		return (CollectionPersister) getJoinable();
	}

	@Override
	public FetchStrategy determineFetchPlan(LoadQueryInfluencers loadQueryInfluencers, PropertyPath propertyPath) {
		final EntityPersister owningPersister = locateOwningPersister();

		FetchStyle style = determineFetchStyleByProfile(
				loadQueryInfluencers,
				owningPersister,
				propertyPath,
				ownerAttributeNumber()
		);
		if ( style == null ) {
			style = determineFetchStyleByMetadata(
					getSource().getType().getFetchMode( attributeNumber() ),
					getType()
			);
		}

		return new FetchStrategy( determineFetchTiming( style ), style );
	}

	protected FetchStyle determineFetchStyleByProfile(
			LoadQueryInfluencers loadQueryInfluencers,
			EntityPersister owningPersister,
			PropertyPath propertyPath,
			int ownerAttributeNumber) {
		return FetchStrategyHelper.determineFetchStyleByProfile(
				loadQueryInfluencers,
				owningPersister,
				propertyPath,
				ownerAttributeNumber
		);
	}

	protected FetchStyle determineFetchStyleByMetadata(FetchMode fetchMode, AssociationType type) {
		return FetchStrategyHelper.determineFetchStyleByMetadata( fetchMode, type, sessionFactory() );
	}

	private FetchTiming determineFetchTiming(FetchStyle style) {
		return FetchStrategyHelper.determineFetchTiming( style, getType(), sessionFactory() );
	}

	private EntityPersister locateOwningPersister() {
		return getSource().locateOwningPersister();
	}

	@Override
	public CascadeStyle determineCascadeStyle() {
		final CompositeType compositeType = (CompositeType) locateOwningPersister().getPropertyType( getName() );
		return compositeType.getCascadeStyle( attributeNumber() );
	}

	private HydratedCompoundValueHandler hydratedCompoundValueHandler;

	@Override
	public HydratedCompoundValueHandler getHydratedCompoundValueExtractor() {
		if ( hydratedCompoundValueHandler == null ) {
			hydratedCompoundValueHandler = new HydratedCompoundValueHandler() {
				@Override
				public Object extract(Object hydratedState) {
					return ( (Object[] ) hydratedState )[ attributeNumber() ];
				}

				@Override
				public void inject(Object hydratedState, Object value) {
					( (Object[] ) hydratedState )[ attributeNumber() ] = value;
				}
			};
		}
		return hydratedCompoundValueHandler;
	}

	@Override
	protected String loggableMetadata() {
		return super.loggableMetadata() + ",association";
	}
}
