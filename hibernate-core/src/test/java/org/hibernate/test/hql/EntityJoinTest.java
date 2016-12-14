/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole, Jan Martiska
 */
public class EntityJoinTest extends BaseNonConfigCoreFunctionalTestCase {
    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] {FinancialRecord.class, User.class, Customer.class};
    }

    @Before
    public void prepare() {
        createTestData();
    }

    @After
    public void cleanup() {
        deleteTestData();
    }

    @Test()
    public void testInnerEntityJoins() {
        doInHibernate( this::sessionFactory, session -> {

            // this should get financial records which have a lastUpdateBy user set
            List<Object[]> result = session.createQuery(
                    "select r.id, c.name, u.id, u.username " +
                            "from FinancialRecord r " +
                            "   inner join r.customer c " +
                            "	inner join User u on r.lastUpdateBy = u.username"
            ).list();

            assertThat( result.size(), is( 1 ) );
            Object[] steveAndAcme = result.get( 0 );
            assertThat( steveAndAcme[0], is( 1 ) );
            assertThat( steveAndAcme[1], is( "Acme" ) );
            assertThat( steveAndAcme[3], is( "steve" ) );

            // NOTE that this leads to not really valid SQL, although some databases might support it /
//			result = session.createQuery(
//					"select r.id, r.customer.name, u.id, u.username " +
//							"from FinancialRecord r " +
//							"	inner join User u on r.lastUpdateBy = u.username"
//			).list();
//			assertThat( result.size(), is( 1 ) );

        } );
    }

    @Test
    public void testLeftOuterEntityJoins() {
        doInHibernate( this::sessionFactory, session -> {
            // this should get all financial records even if their lastUpdateBy user is null
            List<Object[]> result = session.createQuery(
                    "select r.id, u.id, u.username " +
                            "from FinancialRecord r " +
                            "	left join User u on r.lastUpdateBy = u.username" +
                            "   order by r.id"
            ).list();
            assertThat( result.size(), is( 2 ) );

            Object[] stevesRecord = result.get( 0 );
            assertThat( stevesRecord[0], is( 1 ) );
            assertThat( stevesRecord[2], is( "steve" ) );

            Object[] noOnesRecord = result.get( 1 );
            assertThat( noOnesRecord[0], is( 2 ) );
            assertNull( noOnesRecord[2] );
        } );
    }

    @Test
    @TestForIssue(jiraKey = "HHH-11337")
    public void testLeftOuterEntityJoinsWithImplicitInnerJoinInSelectClause() {
        doInHibernate( this::sessionFactory, session -> {
            // this should get all financial records even if their lastUpdateBy user is null
            List<Object[]> result = session.createQuery(
                    "select r.id, u.id, u.username, r.customer.name " +
                            "from FinancialRecord r " +
                            "	left join User u on r.lastUpdateBy = u.username" +
                            "   order by r.id"
            ).list();
            assertThat( result.size(), is( 2 ) );

            Object[] stevesRecord = result.get( 0 );
            assertThat( stevesRecord[0], is( 1 ) );
            assertThat( stevesRecord[2], is( "steve" ) );

            Object[] noOnesRecord = result.get( 1 );
            assertThat( noOnesRecord[0], is( 2 ) );
            assertNull( noOnesRecord[2] );
        } );
    }

    @Test
    @TestForIssue(jiraKey = "HHH-11340")
    public void testJoinOnEntityJoinNode() {
        doInHibernate( this::sessionFactory, session -> {
            // this should get all financial records even if their lastUpdateBy user is null
            List<Object[]> result = session.createQuery(
                    "select u.username, c.name " +
                            "from FinancialRecord r " +
                            "	left join User u on r.lastUpdateBy = u.username " +
                            "   left join u.customer c " +
                            "   order by r.id"
            ).list();
            assertThat( result.size(), is( 2 ) );

            Object[] stevesRecord = result.get( 0 );
            assertThat( stevesRecord[0], is( "steve" ) );
            assertThat( stevesRecord[1], is( "Acme" ) );

            Object[] noOnesRecord = result.get( 1 );
            assertNull( noOnesRecord[0] );
            assertNull( noOnesRecord[1] );
        } );
    }

    @Test
    public void testRightOuterEntityJoins() {
        doInHibernate( this::sessionFactory, session -> {
            // this should get all users even if they have no financial records
            List<Object[]> result = session.createQuery(
                    "select r.id, u.id, u.username " +
                            "from FinancialRecord r " +
                            "	right join User u on r.lastUpdateBy = u.username" +
                            "   order by u.id"
            ).list();

            assertThat( result.size(), is( 2 ) );

            Object[] steveAndAcme = result.get( 0 );
            assertThat( steveAndAcme[0], is( 1 ) );
            assertThat( steveAndAcme[2], is( "steve" ) );

            Object[] janeAndNull = result.get( 1 );
            assertNull( janeAndNull[0] );
            assertThat( janeAndNull[2], is( "jane" ) );
        } );
    }

    private void createTestData() {
        doInHibernate( this::sessionFactory, session -> {

            final Customer customer = new Customer( 1, "Acme" );
            session.save( customer );
            session.save( new User( 1, "steve", customer ) );
            session.save( new User( 2, "jane" ) );
            session.save( new FinancialRecord( 1, customer, "steve" ) );
            session.save( new FinancialRecord( 2, customer, null ) );

        } );
    }

    private void deleteTestData() {
        doInHibernate( this::sessionFactory, session -> {
            session.createQuery( "delete FinancialRecord" ).executeUpdate();
            session.createQuery( "delete User" ).executeUpdate();
            session.createQuery( "delete Customer" ).executeUpdate();

        } );
    }

    @Entity(name = "Customer")
    @Table(name = "customer")
    public static class Customer {
        private Integer id;
        private String name;

        public Customer() {
        }

        public Customer(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        @Id
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Entity(name = "FinancialRecord")
    @Table(name = "financial_record")
    public static class FinancialRecord {
        private Integer id;
        private Customer customer;
        private String lastUpdateBy;

        public FinancialRecord() {
        }

        public FinancialRecord(Integer id, Customer customer, String lastUpdateBy) {
            this.id = id;
            this.customer = customer;
            this.lastUpdateBy = lastUpdateBy;
        }

        @Id
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @ManyToOne
        @JoinColumn
        public Customer getCustomer() {
            return customer;
        }

        public void setCustomer(Customer customer) {
            this.customer = customer;
        }

        public String getLastUpdateBy() {
            return lastUpdateBy;
        }

        public void setLastUpdateBy(String lastUpdateBy) {
            this.lastUpdateBy = lastUpdateBy;
        }
    }

    @Entity(name = "User")
    @Table(name = "`user`")
    public static class User {
        private Integer id;
        private String username;
        private Customer customer;

        public User() {
        }

        public User(Integer id, String username) {
            this.id = id;
            this.username = username;
        }

        public User(Integer id, String username, Customer customer) {
            this.id = id;
            this.username = username;
            this.customer = customer;
        }

        @Id
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        @NaturalId
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        @ManyToOne(fetch = FetchType.LAZY)
        public Customer getCustomer() {
            return customer;
        }

        public void setCustomer(Customer customer) {
            this.customer = customer;
        }
    }


}
