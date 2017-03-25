/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
 * @author Gavin King
 */
public class CollectionElementLoader extends OuterJoinLoader {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			CollectionElementLoader.class.getName()
	);

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
						collectionPersister.toColumns( "index" )
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

	public Object loadElement(SharedSessionContractImplementor session, Object key, Object index)
			throws HibernateException {

		List list = loadEntity(
				session,
				key,
				index,
				keyType,
				indexType,
				persister
		);

		if ( list.size() == 1 ) {
			return list.get( 0 );
		}
		else if ( list.size() == 0 ) {
			return null;
		}
		else {
			if ( getCollectionOwners() != null ) {
				return list.get( 0 );
			}
			else {
				throw new HibernateException( "More than one row was found" );
			}
		}

	}

	@Override
	protected Object getResultColumnOrRow(
			Object[] row,
			ResultTransformer transformer,
			ResultSet rs,
			SharedSessionContractImplementor session) throws SQLException, HibernateException {
		return row[row.length - 1];
	}

	@Override
	protected boolean isSingleRowLoader() {
		return true;
	}
}
