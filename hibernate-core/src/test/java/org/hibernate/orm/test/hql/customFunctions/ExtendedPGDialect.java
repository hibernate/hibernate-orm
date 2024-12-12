package org.hibernate.orm.test.hql.customFunctions;
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgreSQLDriverKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

//tag::hql-user-defined-dialect-function-custom-dialect[]
public class ExtendedPGDialect extends PostgreSQLDialect {

    // Default constructors

    //end::hql-user-defined-dialect-function-custom-dialect[]
    public ExtendedPGDialect() {
        super();
    }

    public ExtendedPGDialect(DialectResolutionInfo info) {
        super(info);
    }

    public ExtendedPGDialect(DatabaseVersion version) {
        super(version);
    }

    public ExtendedPGDialect(DatabaseVersion version, PostgreSQLDriverKind driverKind) {
        super(version, driverKind);
    }

    //tag::hql-user-defined-dialect-function-custom-dialect[]
    //tag::hql-user-defined-dialect-function-registry-extending[]
    @Override
    public void initializeFunctionRegistry(FunctionContributions functionContributions) {
        super.initializeFunctionRegistry(functionContributions);
        //end::hql-user-defined-dialect-function-custom-dialect[]
        // Custom aggregate function
        functionContributions.getFunctionRegistry().register(
            "countItemsGreaterVal", // Name that can be used in JPQL queries
            new CountItemsGreaterValSqmFunction(
                "count_items_greater_val", // Name of the function in the database
                this,
                functionContributions.getTypeConfiguration())
        );
        //tag::hql-user-defined-dialect-function-custom-dialect[]
    }
    //end::hql-user-defined-dialect-function-registry-extending[]
}
//end::hql-user-defined-dialect-function-custom-dialect[]
