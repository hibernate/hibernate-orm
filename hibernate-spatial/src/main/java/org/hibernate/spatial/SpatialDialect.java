/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
