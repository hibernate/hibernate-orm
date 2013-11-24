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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.JoinWalker;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 *
 *
 * @author Gavin King
 */
public class CollectionElementLoader extends OuterJoinLoader {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, CollectionElementLoader.class.getName() );

	private final OuterJoinLoadable persister;
	private final Type keyType;
	private final Type indexType;
	private final String entityName;

	public CollectionElementLoader(
			QueryableCollection collectionPersister,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) throws MappingException {
		super( factory, loadQueryInfluencers );

		this.keyType = collectionPersister.getKeyType();
		this.indexType = collectionPersister.getIndexType();
		this.persister = (OuterJoinLoadable) collectionPersister.getElementPersister();
		this.entityName = persister.getEntityName();

		JoinWalker walker = new EntityJoinWalker(
				persister, 
				ArrayHelper.join(
						collectionPersister.getKeyColumnNames(),
						collectionPersister.toColumns("index")
				),
				1, 
				LockMode.NONE, 
				factory, 
				loadQueryInfluencers
			);
		initFromWalker( walker );

		postInstantiate();

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Static select for entity %s: %s", entityName, getSQLString() );
		}

	}

	public Object loadElement(SessionImplementor session, Object key, Object index)
	throws HibernateException {

		List list = loadEntity(
				session,
				key,
				index,
				keyType,
				indexType,
				persister
			);

		if ( list.size()==1 ) {
			return list.get(0);
		}
		else if ( list.size()==0 ) {
			return null;
		}
		else {
			if ( getCollectionOwners()!=null ) {
				return list.get(0);
			}
			else {
				throw new HibernateException("More than one row was found");
			}
		}

	}

	@Override
    protected Object getResultColumnOrRow(
		Object[] row,
		ResultTransformer transformer,
		ResultSet rs, SessionImplementor session)
	throws SQLException, HibernateException {
		return row[row.length-1];
	}

	@Override
    protected boolean isSingleRowLoader() {
		return true;
	}
}
