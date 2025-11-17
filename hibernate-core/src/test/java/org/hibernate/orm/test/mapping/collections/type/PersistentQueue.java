/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.type;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

//tag::collections-custom-collection-mapping-example[]
public class PersistentQueue extends PersistentBag implements Queue {

	public PersistentQueue(SharedSessionContractImplementor session) {
		super(session);
	}

	public PersistentQueue(SharedSessionContractImplementor session, List list) {
		super(session, list);
	}

	@Override
	public boolean offer(Object o) {
		return add(o);
	}

	@Override
	public Object remove() {
		return poll();
	}

	@Override
	public Object poll() {
		int size = size();
		if(size > 0) {
			Object first = get(0);
			remove(0);
			return first;
		}
		throw new NoSuchElementException();
	}

	@Override
	public Object element() {
		return peek();
	}

	@Override
	public Object peek() {
		return size() > 0 ? get(0) : null;
	}
}
//end::collections-custom-collection-mapping-example[]
