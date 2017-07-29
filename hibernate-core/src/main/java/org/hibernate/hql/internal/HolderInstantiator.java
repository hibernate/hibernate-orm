/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal;
import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import org.hibernate.transform.AliasToBeanConstructorResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.Transformers;

/**
 * @author Gavin King
 */
public final class HolderInstantiator {
		
	public static final HolderInstantiator NOOP_INSTANTIATOR = new HolderInstantiator(null);
	
	private final ResultTransformer transformer;
	private Supplier<String[]> queryReturnAliasesSupplier = () -> null;
	
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
		return new HolderInstantiator( resolveClassicResultTransformer( constructor, transformer ) );
	}

	static public ResultTransformer resolveClassicResultTransformer(
			Constructor constructor,
			ResultTransformer transformer) {
		return constructor != null ? new AliasToBeanConstructorResultTransformer( constructor ) : transformer;
	}

	public HolderInstantiator(ResultTransformer transformer) {
		this.transformer = transformer;
	}

	public HolderInstantiator(ResultTransformer transformer, String[] queryReturnAliases) {
		this.transformer = transformer;		
		this.queryReturnAliasesSupplier = () -> queryReturnAliases;
	}

	public HolderInstantiator(ResultTransformer transformer,  Supplier<String[]> queryReturnAliasesSupplier) {
		this.transformer = transformer;
		this.queryReturnAliasesSupplier = queryReturnAliasesSupplier;
	}
	
	public boolean isRequired() {
		return transformer!=null;
	}
	
	public Object instantiate(Object[] row) {
		if (transformer==null) {
			return row;
		}
		else {
			return transformer.transformTuple(row, getQueryReturnAliases());
		}
	}	
	
	public String[] getQueryReturnAliases() {
		return queryReturnAliasesSupplier.get();
	}

	public ResultTransformer getResultTransformer() {
		return transformer;
	}

}
