/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.custom.parameterized;

import java.util.List;

import org.hibernate.collection.internal.PersistentList;
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
