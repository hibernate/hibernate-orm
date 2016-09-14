/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import javax.persistence.Tuple;

/**
 * An extension to TupleTransformer indicating that we are building a
 * {@link Tuple}.  Hibernate uses this distinction to better understand
 * how this TupleTransformer part in shaping the query result row type.
 *
 * @author Steve Ebersole
 */
public interface TupleBuilder extends TupleTransformer<Tuple> {
}
