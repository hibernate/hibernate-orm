//$Id: EntityLoader.java 7652 2005-07-26 05:51:47Z oneovthafew $
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