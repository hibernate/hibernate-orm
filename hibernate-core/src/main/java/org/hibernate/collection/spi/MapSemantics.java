/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.collection.spi;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Extension of {@link CollectionSemantics} for Maps
 *
 * @author Steve Ebersole
 */
public interface MapSemantics<MKV extends Map<K,V>,K,V> extends CollectionSemantics<MKV,V> {
	Iterator<K> getKeyIterator(MKV rawMap);

	void visitKeys(MKV rawMap, Consumer<? super K> action);

	void visitEntries(MKV rawMap, BiConsumer<? super K,? super V> action);
}
