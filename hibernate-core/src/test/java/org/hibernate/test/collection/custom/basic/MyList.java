/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.custom.basic;

import java.util.ArrayList;

/**
 * A custom collection class. We extend a java.util.Collection class, but that is not required. 
 * It could be totally non-java-collection type, but then we would need to implement all the PersistentCollection methods.
 * 
 * @author max
 */
public class MyList<X> extends ArrayList<X> implements IMyList<X> {
}
