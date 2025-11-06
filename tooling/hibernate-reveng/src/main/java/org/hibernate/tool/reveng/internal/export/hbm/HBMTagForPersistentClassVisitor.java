/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.hbm;

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
