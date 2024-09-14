/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.sqm;

import org.hibernate.query.sqm.spi.DelegatingSqmSelectionQueryImplementor;
import org.hibernate.query.sqm.spi.SqmSelectionQueryImplementor;

/**
 * This class just serves as compilation unit to verify we implemented all methods in the {@link DelegatingSqmSelectionQueryImplementor} class.
 */
public class DelegatingSqmSelectionQueryImplementorTest<R> extends DelegatingSqmSelectionQueryImplementor<R> {
	@Override
	protected SqmSelectionQueryImplementor<R> getDelegate() {
		return null;
	}
}
