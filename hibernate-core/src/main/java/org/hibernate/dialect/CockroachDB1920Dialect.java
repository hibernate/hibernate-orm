package org.hibernate.dialect;

import org.hibernate.dialect.identity.CockroachDB1920IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQL10IdentityColumnSupport;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.persistent.PersistentTableBulkIdStrategy;
import org.hibernate.type.descriptor.sql.BlobTypeDescriptor;
import org.hibernate.type.descriptor.sql.ClobTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.DatabaseMetaData;
import java.sql.Types;

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
        return new PersistentTableBulkIdStrategy();
    }

    @Override
    public boolean supportsExpectedLobUsagePattern() {
        return true;
    }

    @Override
    public boolean useInputStreamToInsertBlob() {
        return true;
    }

    @Override
    public SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
        SqlTypeDescriptor descriptor;
        switch ( sqlCode ) {
            case Types.BLOB: {
                // Force BLOB binding.  Otherwise, byte[] fields annotated
                // with @Lob will attempt to use
                // BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING.  Since the
                // dialect uses oid for Blobs, byte arrays cannot be used.
                descriptor = BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING;
                break;
            }
            case Types.CLOB: {
                descriptor = ClobTypeDescriptor.STREAM_BINDING;
                break;
            }
            default: {
                descriptor = super.getSqlTypeDescriptorOverride( sqlCode );
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
    public boolean supportsSkipLocked() { return false; }

    @Override
    public boolean supportsMixedTypeArithmetic() { return false; }
}
