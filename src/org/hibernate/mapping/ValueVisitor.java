/*
 * Created on 06-Dec-2004
 *
 */
package org.hibernate.mapping;

/**
 * @author max
 *
 */
public interface ValueVisitor {

	/**
	 * @param bag
	 */
	Object accept(Bag bag);

	/**
	 * @param bag
	 */
	Object accept(IdentifierBag bag);

	/**
	 * @param list
	 */
	Object accept(List list);
	
	Object accept(PrimitiveArray primitiveArray);
	Object accept(Array list);

	/**
	 * @param map
	 */
	Object accept(Map map);

	/**
	 * @param many
	 */
	Object accept(OneToMany many);

	/**
	 * @param set
	 */
	Object accept(Set set);

	/**
	 * @param any
	 */
	Object accept(Any any);

	/**
	 * @param value
	 */
	Object accept(SimpleValue value);
	Object accept(DependantValue value);
	
	Object accept(Component component);
	
	Object accept(ManyToOne mto);
	Object accept(OneToOne oto);
	

}
