/*
 * Created on 07-Dec-2004
 *
 */
package org.hibernate.tool.hbm2x.visitor;

import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClassVisitor;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;

/**
 * @author max
 *
 */
public class HBMTagForPersistentClassVisitor implements PersistentClassVisitor {

	public static final PersistentClassVisitor INSTANCE = new HBMTagForPersistentClassVisitor();
	
	protected HBMTagForPersistentClassVisitor() {
		
	}

	public Object accept(RootClass class1) {
		return "class";
	}

	public Object accept(UnionSubclass subclass) {
		return "union-subclass";
	}

	public Object accept(SingleTableSubclass subclass) {
		return "subclass";
	}

	public Object accept(JoinedSubclass subclass) {
		return "joined-subclass";
	}

	public Object accept(Subclass subclass) {
		return "subclass";
	}

	

}
