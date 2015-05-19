/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;


/**
 * @author max
 *
 */
public interface PersistentClassVisitor {


	Object accept(RootClass class1);


	Object accept(UnionSubclass subclass);

	Object accept(SingleTableSubclass subclass);


	Object accept(JoinedSubclass subclass);


	Object accept(Subclass subclass);

	
}
