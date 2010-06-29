package org.hibernate.dialect;

import java.sql.Types;
import java.util.Properties;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;

/**
 * A SQL dialect for Ingres 10 and later versions.
 * <p/>
 * Changes:
 * <ul>
 * <li>Add native BOOLEAN type support</li>
 * </ul>
 *
 * @author Raymond Fan
 */
public class Ingres10Dialect extends Ingres9Dialect {
    public Ingres10Dialect() {
        super();
        registerBooleanSupport();
    }

    // Boolean column type support

    /**
     * The SQL literal value to which this database maps boolean values.
     *
     * @param bool The boolean value
     * @return The appropriate SQL literal.
     */
    public String toBooleanValueString(boolean bool) {
        return bool ? "true" : "false";
    }

    protected void registerBooleanSupport() {
        // Column type

        // Boolean type (mapping/BooleanType) mapping maps SQL BIT to Java
        // Boolean. In order to create a boolean column, BIT needs to be mapped
        // to boolean as well, similar to H2Dialect.
        registerColumnType( Types.BIT, "boolean" );
        registerColumnType( Types.BOOLEAN, "boolean" );

        // Functions

        // true, false and unknown are now valid values
        // Remove the query substitutions previously added in IngresDialect.
        Properties properties = getDefaultProperties();
        String querySubst = properties.getProperty(Environment.QUERY_SUBSTITUTIONS);
        if (querySubst != null) {
            String newQuerySubst = querySubst.replace("true=1,false=0","");
            properties.setProperty(Environment.QUERY_SUBSTITUTIONS, newQuerySubst);
        }
    }
}
