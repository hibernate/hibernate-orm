/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.entity;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.Type;

public abstract class AbstractEntityLoader
		extends OuterJoinLoader
		implements UniqueEntityLoader {
	protected final OuterJoinLoadable persister;
	protected final Type uniqueKeyType;
	protected final String entityName;

	public AbstractEntityLoader(
			OuterJoinLoadable persister,
			Type uniqueKeyType,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( factory, loadQueryInfluencers );
		this.uniqueKeyType = uniqueKeyType;
		this.entityName = persister.getEntityName();
		this.persister = persister;

	}

	@Override
	public Object load(Serializable id, Object optionalObject, SessionImplementor session) {
		// this form is deprecated!
		return load( id, optionalObject, session, LockOptions.NONE );
	}

	@Override
	public Object load(Serializable id, Object optionalObject, SessionImplementor session, LockOptions lockOptions) {
		return load( session, id, optionalObject, id, lockOptions );
	}

	protected Object load(
			SessionImplementor session,
			Object id,
			Object optionalObject,
			Serializable optionalId,
			LockOptions lockOptions) {

		List list = loadEntity(
				session,
				id,
				uniqueKeyType,
				optionalObject,
				entityName,
				optionalId,
				persister,
				lockOptions
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
				throw new HibernateException(
						"More than one row with the given identifier was found: " +
						id +
						", for class: " +
						persister.getEntityName()
					);
			}
		}

	}

	@Override
	protected Object getResultColumnOrRow(
			Object[] row,
			ResultTransformer transformer,
			ResultSet rs,
			SessionImplementor session) throws SQLException, HibernateException {
		return row[row.length-1];
	}

	@Override
	protected boolean isSingleRowLoader() {
		return true;
	}

}
