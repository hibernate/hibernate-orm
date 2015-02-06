package org.hibernate.dialect;

/**
 * An SQL Dialect for PostgreSQL 9.3 and later. Adds support for Materialized view.
 *
 * @author Dionis Argiri
 */
public class PostgreSQL93Dialect extends PostgreSQL9Dialect {
    @Override
    public boolean supportsMaterializedView() {
        return true;
    }

    @Override
    public String getMaterializedViewTypeTerm() {
        return "MATERIALIZED VIEW";
    }
}
