/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.pc;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.Session;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.ParamDef;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class FilterJoinTableTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Client.class,
            Account.class
        };
    }

    @Test
    public void testLifecycle() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::pc-filter-join-table-persistence-example[]
            Client client = new Client()
            .setId( 1L )
            .setName( "John Doe" );

            client.addAccount(
                new Account()
                .setId( 1L )
                .setType( AccountType.CREDIT )
                .setAmount( 5000d )
                .setRate( 1.25 / 100 )
            );

            client.addAccount(
                new Account()
                .setId( 2L )
                .setType( AccountType.DEBIT )
                .setAmount( 0d )
                .setRate( 1.05 / 100 )
            );

            client.addAccount(
                new Account()
                .setType( AccountType.DEBIT )
                .setId( 3L )
                .setAmount( 250d )
                .setRate( 1.05 / 100 )
            );

            entityManager.persist( client );
            //end::pc-filter-join-table-persistence-example[]
        } );

        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::pc-no-filter-join-table-collection-query-example[]
            Client client = entityManager.find( Client.class, 1L );

            assertEquals( 3, client.getAccounts().size());
            //end::pc-no-filter-join-table-collection-query-example[]
        } );

        doInJPA( this::entityManagerFactory, entityManager -> {
            log.infof( "Activate filter [%s]", "firstAccounts");

            //tag::pc-filter-join-table-collection-query-example[]
            Client client = entityManager.find( Client.class, 1L );

            entityManager
                .unwrap( Session.class )
                .enableFilter( "firstAccounts" )
                .setParameter( "maxOrderId", 1);

            assertEquals( 2, client.getAccounts().size());
            //end::pc-filter-join-table-collection-query-example[]
        } );
    }

    public enum AccountType {
        DEBIT,
        CREDIT
    }

    //tag::pc-filter-join-table-example[]
    @Entity(name = "Client")
    @FilterDef(
        name="firstAccounts",
        parameters=@ParamDef(
            name="maxOrderId",
            type="int"
        )
    )
    @Filter(
        name="firstAccounts",
        condition="order_id <= :maxOrderId"
    )
    public static class Client {

        @Id
        private Long id;

        private String name;

        @OneToMany(cascade = CascadeType.ALL)
        @OrderColumn(name = "order_id")
        @FilterJoinTable(
            name="firstAccounts",
            condition="order_id <= :maxOrderId"
        )
        private List<Account> accounts = new ArrayList<>( );

        //Getters and setters omitted for brevity
        //end::pc-filter-join-table-example[]
        public Long getId() {
            return id;
        }

        public Client setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public Client setName(String name) {
            this.name = name;
            return this;
        }

        public List<Account> getAccounts() {
            return accounts;
        }
        //tag::pc-filter-join-table-example[]

        public void addAccount(Account account) {
            this.accounts.add( account );
        }
    }

    @Entity(name = "Account")
    public static class Account {

        @Id
        private Long id;

        @Column(name = "account_type")
        @Enumerated(EnumType.STRING)
        private AccountType type;

        private Double amount;

        private Double rate;

        //Getters and setters omitted for brevity
    //end::pc-filter-join-table-example[]
        public Long getId() {
            return id;
        }

        public Account setId(Long id) {
            this.id = id;
            return this;
        }

        public AccountType getType() {
            return type;
        }

        public Account setType(AccountType type) {
            this.type = type;
            return this;
        }

        public Double getAmount() {
            return amount;
        }

        public Account setAmount(Double amount) {
            this.amount = amount;
            return this;
        }

        public Double getRate() {
            return rate;
        }

        public Account setRate(Double rate) {
            this.rate = rate;
            return this;
        }

        //tag::pc-filter-join-table-example[]
    }
    //end::pc-filter-join-table-example[]
}
