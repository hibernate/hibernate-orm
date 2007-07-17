//$Id: HolderInstantiator.java 9636 2006-03-16 14:14:48Z max.andersen@jboss.com $
package org.hibernate.hql;

import java.lang.reflect.Constructor;

import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;

/**
 * @author Gavin King
 */
public final class HolderInstantiator {
		
	public static final HolderInstantiator NOOP_INSTANTIATOR = new HolderInstantiator(null,null);
	
	private final ResultTransformer transformer;
	private final String[] queryReturnAliases;
	
	public static HolderInstantiator getHolderInstantiator(ResultTransformer selectNewTransformer, ResultTransformer customTransformer, String[] queryReturnAliases) {
		if(selectNewTransformer!=null) {
			return new HolderInstantiator(selectNewTransformer, queryReturnAliases);
		} else {
			return new HolderInstantiator(customTransformer, queryReturnAliases);
		}
	}
	
	public static ResultTransformer createSelectNewTransformer(Constructor constructor, boolean returnMaps, boolean returnLists) {
		if ( constructor != null ) {
			return new AliasToBeanConstructorResultTransformer(constructor);
		}
		else if ( returnMaps ) {
			return Transformers.ALIAS_TO_ENTITY_MAP;			
		}
		else if ( returnLists ) {
			return Transformers.TO_LIST;
		}		
		else {
			return null;
		}
	}
	
	static public HolderInstantiator createClassicHolderInstantiator(Constructor constructor, 
			ResultTransformer transformer) {
		if ( constructor != null ) {
			return new HolderInstantiator(new AliasToBeanConstructorResultTransformer(constructor), null);
		}
		else {
			return new HolderInstantiator(transformer, null);
		}
	}
	
	public HolderInstantiator( 
			ResultTransformer transformer,
			String[] queryReturnAliases
	) {
		this.transformer = transformer;		
		this.queryReturnAliases = queryReturnAliases;
	}
	
	public boolean isRequired() {
		return transformer!=null;
	}
	
	public Object instantiate(Object[] row) {
		if(transformer==null) {
			return row;
		} else {
			return transformer.transformTuple(row, queryReturnAliases);
		}
	}	
	
	public String[] getQueryReturnAliases() {
		return queryReturnAliases;
	}

	public ResultTransformer getResultTransformer() {
		return transformer;
	}

}
