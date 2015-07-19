package org.hibernate.test.annotations.formula;

import org.hibernate.dialect.Oracle12cDialect;

/**
 * Dialect for test case where we register a keyword and see if it gets escaped or not.
 *
 * Created by Mike on 18/07/2015.
 */
public class ExtendedOracleDialect extends Oracle12cDialect {

    public ExtendedOracleDialect() {
        super();
        registerKeyword("integer");
    }

}
