/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.queryhint;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.YugabyteDBDialect;
import org.hibernate.query.Query;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Amogh Shetkar
 */
@RequiresDialect(value = YugabyteDBDialect.class)
@DomainModel(
        annotatedClasses = { YugabyteDBQueryHintTest.Employee.class, YugabyteDBQueryHintTest.Department.class }
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
        settings = @Setting(name = AvailableSettings.USE_SQL_COMMENTS, value = "true")
)
public class YugabyteDBQueryHintTest {

    @BeforeAll
    protected void setUp(SessionFactoryScope scope) {
        Department department = new Department();
        department.name = "Sales";
        Employee employee1 = new Employee();
        employee1.department = department;
        Employee employee2 = new Employee();
        employee2.department = department;

        scope.inTransaction( s -> {
            s.persist( department );
            s.persist( employee1 );
            s.persist( employee2 );
        } );
    }

    @Test
    public void testQueryHint(SessionFactoryScope scope) {
        final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
        statementInspector.clear();

        // test Query with a simple hint
        String hint = "someHint";
        scope.inTransaction( s -> {
            Query query = s.createQuery( "FROM Employee e WHERE e.department.name = :departmentName" )
                    .addQueryHint( hint )
                    .setParameter( "departmentName", "Sales" );
            List results = query.list();

            assertEquals( 2, results.size() );
        } );

        statementInspector.assertExecutedCount( 1 );
        assertTrue( statementInspector.getSqlQueries().get( 0 ).contains( "/*+ " + hint + " */select" ) );
        statementInspector.clear();
    }


    @Entity(name = "Employee")
    public static class Employee {
        @Id
        @GeneratedValue
        public long id;

        @ManyToOne(fetch = FetchType.LAZY)
        public Department department;
    }

    @Entity(name = "Department")
    public static class Department {
        @Id
        @GeneratedValue
        public long id;

        public String name;
    }
}
