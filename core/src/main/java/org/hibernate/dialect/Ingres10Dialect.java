package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;

/**
 * A SQL dialect for Ingres 10 and later versions.
 *
 * Changes:
 * <ul>
 * </ul>
 *
 * @author Raymond Fan
 */
public class Ingres10Dialect extends Ingres9Dialect {
    public Ingres10Dialect() {
        super();
    }
}
