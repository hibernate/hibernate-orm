/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.collection.custom.declaredtype.explicitsemantics;

import java.util.ArrayList;

/**
 * A custom collection class that has both List and Set interfaces, but only really implements set for persistence
 * (e.g. extends PersistentSet). Without setting the semantics on the CollectionType annotation, List semantics
 * would be inferred, and that would not match the implemented methods in PersistentSet and would fail. HeadSetList
 * is very much a toy collection type.
 *
 * @author David Weinberg
 */
public class HeadSetList<X> extends ArrayList<X> implements IHeadSetList<X> {

	@Override
	public X head() {
		return isEmpty() ? null : get( 0 );
	}
}
