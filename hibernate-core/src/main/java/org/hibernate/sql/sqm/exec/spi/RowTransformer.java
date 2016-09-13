/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.exec.spi;

import org.hibernate.Incubating;

/**
 * This is the tuple-transformer that is applied at the execution level.
 * <p/>
 * As a design question, should this encapsulate just dynamic-instantiation and
 * {@link javax.persistence.Tuple} transformations?  Or, should this also
 * cover {@link org.hibernate.query.TupleTransformer}?  If we decide to handle
 * TupleTransformer here, we need to pass along the aliases; we may also
 * need to allow for nestable RowTransformers.  Currently this is only used
 * to encapsulate dynamic-instantiation and {@link javax.persistence.Tuple}
 * transformations
 *
 * @todo Answer the design question above
 *
 * @author Steve Ebersole
 */
@Incubating
public interface RowTransformer<T> {
	T transformRow(Object[] row);
}
