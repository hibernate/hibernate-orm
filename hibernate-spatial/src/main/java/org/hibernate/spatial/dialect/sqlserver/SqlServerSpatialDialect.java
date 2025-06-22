/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.sqlserver;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.spatial.SpatialDialect;

/**
 * Created by Karel Maesen, Geovise BVBA on 19/09/2018.
 * @deprecated A SpatialDialect is no longer required. Use the standard Dialect for this database.
 */
@Deprecated
public class SqlServerSpatialDialect extends SQLServerDialect implements SpatialDialect {

}
