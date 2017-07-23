/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import org.hibernate.query.spi.SelectQueryPlan;

/**
 * Union of {@link SelectQueryPlan} and {@link NativeQueryPlan} as the
 * {@link SelectQueryPlan} for native-queries.
 *
 * @author Steve Ebersole
 */
public interface NativeSelectQueryPlan<T> extends SelectQueryPlan<T>, NativeQueryPlan {
}
