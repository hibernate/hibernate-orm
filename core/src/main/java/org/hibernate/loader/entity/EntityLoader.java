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

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.JoinWalker;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.Type;

/**
 * Loads an entity instance using outerjoin fetching to fetch associated entities.
 * <br>
 * The <tt>EntityPersister</tt> must implement <tt>Loadable</tt>. For other entities,
 * create a customized subclass of <tt>Loader</tt>.
 *
 * @author Gavin King
 */
public class EntityLoader extends AbstractEntityLoader {
	
	private final boolean batchLoader;
	
	public EntityLoader(
			OuterJoinLoadable persister, 
			LockMode lockMode,
			SessionFactoryImplementor factory, 
			Map enabledFilters) 
	throws MappingException {
		this(persister, 1, lockMode, factory, enabledFilters);
	}
	
	public EntityLoader(
			OuterJoinLoadable persister, 
			int batchSize, 
			LockMode lockMode,
			SessionFactoryImplementor factory, 
			Map enabledFilters) 
	throws MappingException {
		this( 
				persister, 
				persister.getIdentifierColumnNames(), 
				persister.getIdentifierType(), 
				batchSize,
				lockMode,
				factory, 
				enabledFilters 
			);
	}

	public EntityLoader(
			OuterJoinLoadable persister, 
			String[] uniqueKey, 
			Type uniqueKeyType, 
			int batchSize, 
			LockMode lockMode,
			SessionFactoryImplementor factory, 
			Map enabledFilters) 
	throws MappingException {
		super(persister, uniqueKeyType, factory, enabledFilters);

		JoinWalker walker = new EntityJoinWalker(
				persister, 
				uniqueKey, 
				batchSize, 
				lockMode, 
				factory, 
				enabledFilters
			);
		initFromWalker( walker );

		postInstantiate();

		batchLoader = batchSize > 1;
		
		log.debug( "Static select for entity " + entityName + ": " + getSQLString() );

	}

	public Object loadByUniqueKey(SessionImplementor session, Object key) 
	throws HibernateException {
		return load(session, key, null, null);
	}

	protected boolean isSingleRowLoader() {
		return !batchLoader;
	}
	
}