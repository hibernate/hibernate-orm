//$Id: SubselectOneToManyLoader.java 7670 2005-07-29 05:36:14Z oneovthafew $
package org.hibernate.loader.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;

/**
 * Implements subselect fetching for a one to many association
 * @author Gavin King
 */
public class SubselectOneToManyLoader extends OneToManyLoader {
	
	private final Serializable[] keys;
	private final Type[] types;
	private final Object[] values;
	private final Map namedParameters;
	private final Map namedParameterLocMap;

	public SubselectOneToManyLoader(
			QueryableCollection persister, 
			String subquery,
			Collection entityKeys,
			QueryParameters queryParameters,
			Map namedParameterLocMap,
			SessionFactoryImplementor factory, 
			Map enabledFilters)
	throws MappingException {
		
		super(persister, 1, subquery, factory, enabledFilters);

		keys = new Serializable[ entityKeys.size() ];
		Iterator iter = entityKeys.iterator();
		int i=0;
		while ( iter.hasNext() ) {
			keys[i++] = ( (EntityKey) iter.next() ).getIdentifier();
		}
		
		this.namedParameters = queryParameters.getNamedParameters();
		this.types = queryParameters.getFilteredPositionalParameterTypes();
		this.values = queryParameters.getFilteredPositionalParameterValues();
		this.namedParameterLocMap = namedParameterLocMap;
		
	}

	public void initialize(Serializable id, SessionImplementor session)
	throws HibernateException {
		loadCollectionSubselect( 
				session, 
				keys, 
				values,
				types,
				namedParameters,
				getKeyType() 
			);
	}

	public int[] getNamedParameterLocs(String name) {
		return (int[]) namedParameterLocMap.get( name );
	}

}
