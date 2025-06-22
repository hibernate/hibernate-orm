/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

/**
 * @author max
 */
public interface PersistentClassVisitor {
	Object accept(RootClass class1);
	Object accept(UnionSubclass subclass);
	Object accept(SingleTableSubclass subclass);
	Object accept(JoinedSubclass subclass);
	Object accept(Subclass subclass);
}
