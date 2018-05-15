/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.collection.spi;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public interface MapSemantics<M> extends CollectionSemantics<M> {
	Iterator getKeyIterator(M rawMap);

	void visitKeys(M rawMap, Consumer action);

	void visitEntries(M rawMap, BiConsumer action);
}
