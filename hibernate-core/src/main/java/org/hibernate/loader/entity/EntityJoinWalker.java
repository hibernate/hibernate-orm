/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.entity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.AbstractEntityJoinWalker;
import org.hibernate.loader.OuterJoinableAssociation;
import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * A walker for loaders that fetch entities
 *
 * @see EntityLoader
 * @author Gavin King
 */
public class EntityJoinWalker extends AbstractEntityJoinWalker {
	
	private final LockOptions lockOptions = new LockOptions();
	private final int[][] compositeKeyManyToOneTargetIndices;

	public EntityJoinWalker(
			OuterJoinLoadable persister, 
			String[] uniqueKey, 
			int batchSize, 
			LockMode lockMode,
			final SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( persister, factory, loadQueryInfluencers );

		this.lockOptions.setLockMode(lockMode);
		
		StringBuilder whereCondition = whereString( getAlias(), uniqueKey, batchSize )
				//include the discriminator and class-level where, but not filters
				.append( persister.filterFragment( getAlias(), Collections.EMPTY_MAP ) );

		AssociationInitCallbackImpl callback = new AssociationInitCallbackImpl( factory );
		initAll( whereCondition.toString(), "", lockOptions, callback );
		this.compositeKeyManyToOneTargetIndices = callback.resolve();
	}

	public EntityJoinWalker(
			OuterJoinLoadable persister,
			String[] uniqueKey,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( persister, factory, loadQueryInfluencers );
		LockOptions.copy(lockOptions, this.lockOptions);

		StringBuilder whereCondition = whereString( getAlias(), uniqueKey, batchSize )
				//include the discriminator and class-level where, but not filters
				.append( persister.filterFragment( getAlias(), Collections.EMPTY_MAP ) );

		AssociationInitCallbackImpl callback = new AssociationInitCallbackImpl( factory );
		initAll( whereCondition.toString(), "", lockOptions, callback );
		this.compositeKeyManyToOneTargetIndices = callback.resolve();
	}

	protected JoinType getJoinType(
			OuterJoinLoadable persister,
			PropertyPath path,
			int propertyNumber,
			AssociationType associationType,
			FetchMode metadataFetchMode,
			CascadeStyle metadataCascadeStyle,
			String lhsTable,
			String[] lhsColumns,
			boolean nullable,
			int currentDepth) throws MappingException {
		// NOTE : we override this form here specifically to account for
		// fetch profiles.
		// TODO : how to best handle criteria queries?
		if ( lockOptions.getLockMode().greaterThan( LockMode.READ ) ) {
			return JoinType.NONE;
		}
		if ( isTooDeep( currentDepth )
				|| ( associationType.isCollectionType() && isTooManyCollections() ) ) {
			return JoinType.NONE;
		}
		if ( !isJoinedFetchEnabledInMapping( metadataFetchMode, associationType )
				&& !isJoinFetchEnabledByProfile( persister, path, propertyNumber ) ) {
			return JoinType.NONE;
		}
		if ( isDuplicateAssociation( lhsTable, lhsColumns, associationType ) ) {
			return JoinType.NONE;
		}
		return getJoinType( nullable, currentDepth );
	}

	public String getComment() {
		return "load " + getPersister().getEntityName();
	}

	public int[][] getCompositeKeyManyToOneTargetIndices() {
		return compositeKeyManyToOneTargetIndices;
	}

	private static class AssociationInitCallbackImpl implements AssociationInitCallback {
		private final SessionFactoryImplementor factory;
		private final HashMap<String,OuterJoinableAssociation> associationsByAlias
				= new HashMap<String, OuterJoinableAssociation>();
		private final HashMap<String,Integer> positionsByAlias = new HashMap<String, Integer>();
		private final ArrayList<String> aliasesForAssociationsWithCompositesIds
				= new ArrayList<String>();

		public AssociationInitCallbackImpl(SessionFactoryImplementor factory) {
			this.factory = factory;
		}

		public void associationProcessed(OuterJoinableAssociation oja, int position) {
			associationsByAlias.put( oja.getRhsAlias(), oja );
			positionsByAlias.put( oja.getRhsAlias(), position );
			EntityPersister entityPersister = null;
			if ( oja.getJoinableType().isCollectionType() ) {
				entityPersister = ( ( QueryableCollection) oja.getJoinable() ).getElementPersister();
			}
			else if ( oja.getJoinableType().isEntityType() ) {
				entityPersister = ( EntityPersister ) oja.getJoinable();
			}
			if ( entityPersister != null
					&& entityPersister.getIdentifierType().isComponentType()
					&& ! entityPersister.getEntityMetamodel().getIdentifierProperty().isEmbedded()
					&& hasAssociation( (CompositeType) entityPersister.getIdentifierType() ) ) {
				aliasesForAssociationsWithCompositesIds.add( oja.getRhsAlias() );
			}
		}

		private boolean hasAssociation(CompositeType componentType) {
			for ( Type subType : componentType.getSubtypes() ) {
				if ( subType.isEntityType() ) {
					return true;
				}
				else if ( subType.isComponentType() && hasAssociation( ( (CompositeType) subType ) ) ) {
					return true;
				}
			}
			return false;
		}

		public int[][] resolve() {
			int[][] compositeKeyManyToOneTargetIndices = null;
			for ( final String aliasWithCompositeId : aliasesForAssociationsWithCompositesIds ) {
				final OuterJoinableAssociation joinWithCompositeId = associationsByAlias.get( aliasWithCompositeId );
				final ArrayList<Integer> keyManyToOneTargetIndices = new ArrayList<Integer>();
				// for each association with a composite id containing key-many-to-one(s), find the bidirectional side of
				// each key-many-to-one (if exists) to see if it is eager as well.  If so, we need to track the indices
				EntityPersister entityPersister = null;
				if ( joinWithCompositeId.getJoinableType().isCollectionType() ) {
					entityPersister = ( ( QueryableCollection) joinWithCompositeId.getJoinable() ).getElementPersister();
				}
				else if ( joinWithCompositeId.getJoinableType().isEntityType() ) {
					entityPersister = ( EntityPersister ) joinWithCompositeId.getJoinable();
				}

				findKeyManyToOneTargetIndices(
						keyManyToOneTargetIndices,
						joinWithCompositeId,
						(CompositeType) entityPersister.getIdentifierType()
				);

				if ( ! keyManyToOneTargetIndices.isEmpty() ) {
					if ( compositeKeyManyToOneTargetIndices == null ) {
						compositeKeyManyToOneTargetIndices = new int[ associationsByAlias.size() ][];
					}
					int position = positionsByAlias.get( aliasWithCompositeId );
					compositeKeyManyToOneTargetIndices[position] = new int[ keyManyToOneTargetIndices.size() ];
					int i = 0;
					for ( int index : keyManyToOneTargetIndices ) {
						compositeKeyManyToOneTargetIndices[position][i] = index;
						i++;
					}
				}
			}
			return compositeKeyManyToOneTargetIndices;
		}

		private void findKeyManyToOneTargetIndices(
				ArrayList<Integer> keyManyToOneTargetIndices,
				OuterJoinableAssociation joinWithCompositeId,
				CompositeType componentType) {
			for ( Type subType : componentType.getSubtypes() ) {
				if ( subType.isEntityType() ) {
					Integer index = locateKeyManyToOneTargetIndex( joinWithCompositeId, (EntityType) subType );
					if ( index != null ) {
						keyManyToOneTargetIndices.add( index );
					}
				}
				else if ( subType.isComponentType() ) {
					findKeyManyToOneTargetIndices(
							keyManyToOneTargetIndices,
							joinWithCompositeId,
							(CompositeType) subType
					);
				}
			}
		}

		private Integer locateKeyManyToOneTargetIndex(OuterJoinableAssociation joinWithCompositeId, EntityType keyManyToOneType) {
			// the lhs (if one) is a likely candidate
			if ( joinWithCompositeId.getLhsAlias() != null ) {
				final OuterJoinableAssociation lhs = associationsByAlias.get( joinWithCompositeId.getLhsAlias() );
				if ( keyManyToOneType.getAssociatedEntityName( factory ).equals( lhs.getJoinableType().getAssociatedEntityName( factory ) ) ) {
					return positionsByAlias.get( lhs.getRhsAlias() );
				}
			}
			// otherwise, seek out OuterJoinableAssociation which are RHS of given OuterJoinableAssociation
			// (joinWithCompositeId)
			for ( OuterJoinableAssociation oja : associationsByAlias.values() ) {
				if ( oja.getLhsAlias() != null && oja.getLhsAlias().equals( joinWithCompositeId.getRhsAlias() ) ) {
					if ( keyManyToOneType.equals( oja.getJoinableType() ) ) {
						return positionsByAlias.get( oja.getLhsAlias() );
					}
				}
			}
			return null;
		}
	}

}
