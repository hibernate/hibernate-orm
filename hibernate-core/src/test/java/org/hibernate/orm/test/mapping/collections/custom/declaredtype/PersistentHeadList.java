/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import org.hibernate.collection.spi.PersistentList;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class PersistentHeadList extends PersistentList implements IHeadList {

	public PersistentHeadList(SharedSessionContractImplementor session) {
		super( session );
	}

	public PersistentHeadList(SharedSessionContractImplementor session, IHeadList list) {
		super( session, list );
	}


	@Override
	public Object head() {
		return ( (IHeadList) list ).head();
	}
}
