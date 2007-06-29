/*
 * Created on 07-Dec-2004
 *
 */
package org.hibernate.mapping;

/**
 * @author max
 *
 */
public interface PersistentClassVisitor {

	/**
	 * @param class1
	 * @return
	 */
	Object accept(RootClass class1);

	/**
	 * @param subclass
	 * @return
	 */
	Object accept(UnionSubclass subclass);

	/**
	 * @param subclass
	 * @return
	 */
	Object accept(SingleTableSubclass subclass);

	/**
	 * @param subclass
	 * @return
	 */
	Object accept(JoinedSubclass subclass);

	/**
	 * @param subclass
	 * @return
	 */
	Object accept(Subclass subclass);

	
}
