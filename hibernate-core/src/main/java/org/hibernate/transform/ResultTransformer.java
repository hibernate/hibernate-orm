/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.transform;

import java.io.Serializable;

import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

/**
 * Implementors define a strategy for transforming query results into the
 * actual application-visible query result list.
 *
 * @author Gavin King
 *
 * @deprecated ResultTransformer is no longer supported.  It has been split
 * into {@link TupleTransformer} and {@link ResultListTransformer} to define
 * functional interfaces.
 */
@Deprecated
public interface ResultTransformer extends TupleTransformer, ResultListTransformer, Serializable {
}
