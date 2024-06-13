/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql.customFunctions;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.Test;


import java.sql.Statement;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@RequiresDialect(PostgreSQLDialect.class)
public class CustomDialectFunctionTest extends BaseCoreFunctionalTestCase {

    @Override
    protected void configure(Configuration configuration) {
        super.configure(configuration);
        configuration.addAnnotatedClass(Employee.class);

        configuration.setProperty(AvailableSettings.DIALECT, "org.hibernate.orm.test.hql.customFunctions.ExtendedPGDialect");
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{
                Employee.class
        };
    }

    @Test
    @RequiresDialect(PostgreSQLDialect.class)
    public void test_custom_sqm_functions() {
        doInJPA(this::sessionFactory, session -> {
            try (EntityManager entityManager = session.getEntityManagerFactory().createEntityManager()) {
                var tx = entityManager.getTransaction();
                tx.begin();

                entityManager.unwrap(Session.class).doWork(connection -> {
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate(
                                "create or replace function greater_than(c bigint, val numeric, gr_val numeric) returns bigint as $$ begin return case when val > gr_val then (c + 1)::bigint else c::bigint end; end; $$ language 'plpgsql'; " +
                                "create or replace function agg_final(c bigint) returns bigint as $$ begin return c; end; $$ language 'plpgsql'; " +
                                "create or replace aggregate count_items_greater_val(numeric, numeric) (sfunc = greater_than, stype = bigint, finalfunc = agg_final, initcond = 0);"
                        );
                    }
                });

                //tag::hql-user-defined-dialect-function-inital-data[]
                entityManager.persist(new Employee(1L, 200L, "Jonn", "Robson"));
                entityManager.persist(new Employee(2L, 350L, "Bert", "Marshall"));
                entityManager.persist(new Employee(3L, 360L, "Joey", "Barton"));
                entityManager.persist(new Employee(4L, 400L, "Bert", "Marshall"));
                //end::hql-user-defined-dialect-function-inital-data[]

                tx.commit();
                //tag::hql-user-defined-dialect-function-test[]
                var res = entityManager
                        .createQuery("select count_items_greater_val(salary, 220) from Employee")
                        .getSingleResult();
                assertEquals(3L, res);
                //end::hql-user-defined-dialect-function-test[]
            }
        });
    }

}
