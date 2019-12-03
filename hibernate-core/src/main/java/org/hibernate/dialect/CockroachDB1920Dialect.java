package org.hibernate.dialect;

import org.hibernate.dialect.identity.CockroachDB1920IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQL10IdentityColumnSupport;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.persistent.PersistentTableBulkIdStrategy;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
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
                // Make BLOBs use byte[] storage.
                descriptor = CockroachDBBlobTypeDescriptor.INSTANCE;
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

    public static final class CockroachDBBlobTypeDescriptor implements SqlTypeDescriptor {

        public static final CockroachDBBlobTypeDescriptor INSTANCE = new CockroachDBBlobTypeDescriptor();

        private CockroachDBBlobTypeDescriptor() {
        }

        @Override
        public int getSqlType() {
            return Types.BLOB;
        }

        @Override
        public boolean canBeRemapped() {
            return true;
        }

        @Override
        public <X> ValueExtractor<X> getExtractor(final JavaTypeDescriptor<X> javaTypeDescriptor) {
            return new BasicExtractor<X>( javaTypeDescriptor, this ) {
                @Override
                protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
                    return javaTypeDescriptor.wrap( rs.getBytes( name ), options );
                }

                @Override
                protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                    return javaTypeDescriptor.wrap( statement.getBytes( index ), options );
                }

                @Override
                protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                    return javaTypeDescriptor.wrap( statement.getBytes( name ), options );
                }
            };
        }

        @Override
        public <X> BasicBinder<X> getBinder(final JavaTypeDescriptor<X> javaTypeDescriptor) {
            return new BasicBinder<X>( javaTypeDescriptor, this ) {
                @Override
                public void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
                        throws SQLException {
                    st.setBytes( index, javaTypeDescriptor.unwrap( value, byte[].class, options ) );
                }

                @Override
                protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
                        throws SQLException {
                    st.setBytes( name, javaTypeDescriptor.unwrap( value, byte[].class, options ) );
                }
            };
        }
    };
}
