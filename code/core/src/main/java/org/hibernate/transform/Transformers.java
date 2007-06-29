package org.hibernate.transform;

final public class Transformers {

	private Transformers() {}
	
	/**
	 * Each row of results is a <tt>Map</tt> from alias to values/entities
	 */
	public static final ResultTransformer ALIAS_TO_ENTITY_MAP = new AliasToEntityMapResultTransformer();

	/**
	 * Each row of results is a <tt>List</tt> 
	 */
	public static final ResultTransformer TO_LIST = ToListResultTransformer.INSTANCE;
	
	/**
	 * Creates a resulttransformer that will inject aliased values into 
	 * instances of Class via property methods or fields.
	 */
	public static ResultTransformer aliasToBean(Class target) {
		return new AliasToBeanResultTransformer(target);
	}
	
}
