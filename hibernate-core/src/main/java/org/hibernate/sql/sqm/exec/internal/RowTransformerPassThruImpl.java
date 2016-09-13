/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.internal;

import org.hibernate.Incubating;
import org.hibernate.sql.sqm.exec.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
@Incubating
public class RowTransformerPassThruImpl implements RowTransformer<Object[]> {
	/**
	 * Singleton access
	 */
	public static final RowTransformerPassThruImpl INSTANCE = new RowTransformerPassThruImpl();

	@Override
	public Object[] transformRow(Object[] row) {
		return row;
	}
}
