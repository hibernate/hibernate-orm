/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.parameterized;

import java.util.List;

import org.hibernate.collection.spi.PersistentList;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * The "persistent wrapper" around our specialized collection contract
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public class PersistentDefaultableList extends PersistentList implements DefaultableList {
	public PersistentDefaultableList(SharedSessionContractImplementor session) {
		super( session );
	}

	public PersistentDefaultableList(SharedSessionContractImplementor session, List list) {
		super( session, list );
	}

	public PersistentDefaultableList() {
	}

	public String getDefaultValue() {
		return ( ( DefaultableList ) this.list ).getDefaultValue();
	}
}
