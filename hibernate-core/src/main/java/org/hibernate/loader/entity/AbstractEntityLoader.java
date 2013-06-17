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

public abstract class AbstractEntityLoader extends OuterJoinLoader
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
    protected Object getResultColumnOrRow(Object[] row, ResultTransformer transformer, ResultSet rs, SessionImplementor session)
	throws SQLException, HibernateException {
		return row[row.length-1];
	}

	@Override
    protected boolean isSingleRowLoader() {
		return true;
	}

}
