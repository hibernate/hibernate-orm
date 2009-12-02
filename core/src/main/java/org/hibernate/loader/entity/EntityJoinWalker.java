/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.loader.entity;

import java.util.Collections;

import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.LockOptions;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.LoadQueryInfluencers;
import org.hibernate.loader.AbstractEntityJoinWalker;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.AssociationType;

/**
 * A walker for loaders that fetch entities
 *
 * @see EntityLoader
 * @author Gavin King
 */
public class EntityJoinWalker extends AbstractEntityJoinWalker {
	
	private final LockOptions lockOptions = new LockOptions();

	public EntityJoinWalker(
			OuterJoinLoadable persister, 
			String[] uniqueKey, 
			int batchSize, 
			LockMode lockMode,
			SessionFactoryImplementor factory, 
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( persister, factory, loadQueryInfluencers );

		this.lockOptions.setLockMode(lockMode);
		
		StringBuffer whereCondition = whereString( getAlias(), uniqueKey, batchSize )
				//include the discriminator and class-level where, but not filters
				.append( persister.filterFragment( getAlias(), Collections.EMPTY_MAP ) );

		initAll( whereCondition.toString(), "", lockOptions);
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

		StringBuffer whereCondition = whereString( getAlias(), uniqueKey, batchSize )
				//include the discriminator and class-level where, but not filters
				.append( persister.filterFragment( getAlias(), Collections.EMPTY_MAP ) );

		initAll( whereCondition.toString(), "", lockOptions);
	}

	protected int getJoinType(
			OuterJoinLoadable persister,
			String path,
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
			return -1;
		}
		if ( isTooDeep( currentDepth )
				|| ( associationType.isCollectionType() && isTooManyCollections() ) ) {
			return -1;
		}
		if ( !isJoinedFetchEnabledInMapping( metadataFetchMode, associationType )
				&& !isJoinFetchEnabledByProfile( persister, path, propertyNumber ) ) {
			return -1;
		}
		if ( isDuplicateAssociation( lhsTable, lhsColumns, associationType ) ) {
			return -1;
		}
		return getJoinType( nullable, currentDepth );
	}

	public String getComment() {
		return "load " + getPersister().getEntityName();
	}
	
}