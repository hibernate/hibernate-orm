/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial;

import java.io.Serializable;

/**
 * Describes the features of a spatially enabled dialect.
 *
 * @author Karel Maesen
 * @deprecated SpatialDialects are no longer needed since Hibernate 6.0
 */
@Deprecated
public interface SpatialDialect extends Serializable {

}
