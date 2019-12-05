/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.identity.CockroachDB1920IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.inline.InlineIdsInClauseBulkIdStrategy;
import org.hibernate.type.descriptor.sql.*;

import java.sql.*;

import java.sql.DatabaseMetaData;

/**
 * An SQL dialect for CockroachDB 19.2 and later. This is the first dialect for CockroachDB. It extends
 * {@link PostgreSQL95Dialect} because CockroachDB is aiming for Postgres compatibility.
 */
public class CockroachDB1920Dialect extends PostgreSQL95Dialect {

    public CockroachDB1920Dialect() {
        super();
        registerColumnType( Types.INTEGER, "int8" );
        registerColumnType( Types.FLOAT, "float8" );
        registerColumnType( Types.BLOB, "bytea" );
    }

    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new CockroachDB1920IdentityColumnSupport();
    }

    @Override
    public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {

        // CockroachDB 19.2.0 does not support temporary tables, so we must override the Postgres behavior.
        return new InlineIdsInClauseBulkIdStrategy();
    }

    public boolean doesReadCommittedCauseWritersToBlockReaders() {
        return true;
    }

    @Override
    public boolean supportsExpectedLobUsagePattern() {
        return false;
    }

    @Override
    public SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
        SqlTypeDescriptor descriptor;
        switch (sqlCode) {
            case Types.BLOB: {
                // Make BLOBs use byte[] storage.
                descriptor = VarbinaryTypeDescriptor.INSTANCE;
                break;
            }
            case Types.CLOB: {
                // Make CLOBs use string storage.
                descriptor = VarcharTypeDescriptor.INSTANCE;
                break;
            }
            default: {
                descriptor = super.getSqlTypeDescriptorOverride(sqlCode);
                break;
            }
        }
        return descriptor;
    }

    @Override
    public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) { return false; }

    @Override
    public boolean supportsLockTimeouts() { return false; }

    @Override
    public boolean supportsSkipLocked() {
        // CockroachDB doesn't support this: https://github.com/cockroachdb/cockroach/issues/40476
        return false;
    }

    @Override
    public boolean supportsNoWait() {
        // CockroachDB doesn't support this: https://github.com/cockroachdb/cockroach/issues/40476
        return false;
    }

    @Override
    public boolean supportsMixedTypeArithmetic() {
        return false;
    }

    @Override
    public boolean canCreateSchema() { return false; }

    @Override
    public boolean supportsStoredProcedures() { return false; }

    @Override
    public boolean supportsComputedIndexes() { return false; }

    @Override
    public boolean supportsLoFunctions() { return false; }
}
