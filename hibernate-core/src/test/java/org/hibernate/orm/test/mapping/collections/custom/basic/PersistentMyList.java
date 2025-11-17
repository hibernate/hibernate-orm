/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.basic;

import org.hibernate.collection.spi.PersistentList;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

public class PersistentMyList extends PersistentList implements IMyList {

	public PersistentMyList(SharedSessionContractImplementor session) {
		super( session );
	}

	public PersistentMyList(SharedSessionContractImplementor session, IMyList list) {
		super( session, list );
	}



}
