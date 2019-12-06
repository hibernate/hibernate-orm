/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import java.sql.Types;

public class CockroachDB1920IdentityColumnSupport extends IdentityColumnSupportImpl {
    @Override
    public boolean supportsIdentityColumns() {
        // Full support requires setting the sql.defaults.serial_normalization=sql_sequence in CockroachDB.
        return false;
    }

    @Override
    public String getIdentitySelectString(String table, String column, int type) {
        return "select currval('" + table + '_' + column + "_seq')";
    }

    @Override
    public String getIdentityColumnString(int type) {
        return type == Types.BIGINT ?
                "bigserial not null" :
                "serial not null";
    }

    @Override
    public boolean hasDataTypeInIdentityColumn() {
        return false;
    }
}
