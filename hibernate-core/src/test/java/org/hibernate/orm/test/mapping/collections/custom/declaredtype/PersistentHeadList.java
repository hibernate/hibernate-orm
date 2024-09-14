/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
