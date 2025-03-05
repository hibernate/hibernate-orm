/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
