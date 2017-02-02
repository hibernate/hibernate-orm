/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.transform;
import java.util.List;

/**
 * Provides the basic "noop" impls of the {@link ResultTransformer} contract.
 *
 * @author Steve Ebersole
 */
public abstract class BasicTransformerAdapter implements ResultTransformer {
	public Object transformTuple(Object[] tuple, String[] aliases) {
		return tuple;
	}

	public List transformList(List list) {
		return list;
	}
}
