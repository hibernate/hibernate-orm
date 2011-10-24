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
package org.hibernate.hql.internal;
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
		return new HolderInstantiator(
				resolveResultTransformer( selectNewTransformer, customTransformer ),
				queryReturnAliases
		);
	}

	public static ResultTransformer resolveResultTransformer(ResultTransformer selectNewTransformer, ResultTransformer customTransformer) {
		return selectNewTransformer != null ? selectNewTransformer : customTransformer;
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
		return new HolderInstantiator( resolveClassicResultTransformer( constructor, transformer ), null );
	}

	static public ResultTransformer resolveClassicResultTransformer(
			Constructor constructor,
			ResultTransformer transformer) {
		return constructor != null ? new AliasToBeanConstructorResultTransformer( constructor ) : transformer;
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
