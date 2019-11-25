package org.hibernate.dialect;

import org.hibernate.dialect.identity.CockroachDB1920IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQL10IdentityColumnSupport;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.persistent.PersistentTableBulkIdStrategy;

/**
 * An SQL dialect for CockroachDB 19.2 and later. This is the first dialect for CockroachDB. It extends
 * {@link PostgreSQL95Dialect} because CockroachDB is aiming for Postgres compatibility.
 */
public class CockroachDB1920Dialect extends PostgreSQL95Dialect {

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
        return false;
    }

    @Override
    public boolean supportsLockTimeouts() {
        return false;
    }
}
