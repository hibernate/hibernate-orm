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
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.OneToMany;

import org.hibernate.Session;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Where;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
public class FilterTest extends BaseEntityManagerFunctionalTestCase {

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

            //tag::pc-filter-persistence-example[]
            Client client = new Client()
            .setId( 1L )
            .setName( "John Doe" );

            client.addAccount(
                new Account()
                .setId( 1L )
                .setType( AccountType.CREDIT )
                .setAmount( 5000d )
                .setRate( 1.25 / 100 )
                .setActive( true )
            );

            client.addAccount(
                new Account()
                .setId( 2L )
                .setType( AccountType.DEBIT )
                .setAmount( 0d )
                .setRate( 1.05 / 100 )
                .setActive( false )
            );

            client.addAccount(
                new Account()
                .setType( AccountType.DEBIT )
                .setId( 3L )
                .setAmount( 250d )
                .setRate( 1.05 / 100 )
                .setActive( true )
            );

            entityManager.persist( client );
            //end::pc-filter-persistence-example[]
        } );

        doInJPA( this::entityManagerFactory, entityManager -> {
            log.infof( "Activate filter [%s]", "activeAccount");

            entityManager
                    .unwrap( Session.class )
                    .enableFilter( "activeAccount" )
                    .setParameter( "active", true);

            Account account1 = entityManager.find( Account.class, 1L );
            Account account2 = entityManager.find( Account.class, 2L );

            assertNotNull( account1 );
            assertNotNull( account2 );
        } );

        doInJPA( this::entityManagerFactory, entityManager -> {
            log.infof( "Activate filter [%s]", "activeAccount");

            entityManager
                    .unwrap( Session.class )
                    .enableFilter( "activeAccount" )
                    .setParameter( "active", true);

            Account account1 = entityManager.createQuery(
                    "select a from Account a where a.id = :id", Account.class)
                    .setParameter( "id", 1L )
                    .getSingleResult();
            assertNotNull( account1 );
            try {
                Account account2 = entityManager.createQuery(
                        "select a from Account a where a.id = :id", Account.class)
                        .setParameter( "id", 2L )
                        .getSingleResult();
            }
            catch (NoResultException expected) {
            }
        } );

        doInJPA( this::entityManagerFactory, entityManager -> {
            log.infof( "Activate filter [%s]", "activeAccount");
            //tag::pc-filter-entity-example[]
            entityManager
                .unwrap( Session.class )
                .enableFilter( "activeAccount" )
                .setParameter( "active", true);

            Account account = entityManager.find( Account.class, 2L );

            assertFalse( account.isActive() );
            //end::pc-filter-entity-example[]
        } );

        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::pc-no-filter-entity-query-example[]
            List<Account> accounts = entityManager.createQuery(
                "select a from Account a", Account.class)
            .getResultList();

            assertEquals( 3, accounts.size());
            //end::pc-no-filter-entity-query-example[]
        } );

        doInJPA( this::entityManagerFactory, entityManager -> {
            log.infof( "Activate filter [%s]", "activeAccount");
            //tag::pc-filter-entity-query-example[]
            entityManager
                .unwrap( Session.class )
                .enableFilter( "activeAccount" )
                .setParameter( "active", true);

            List<Account> accounts = entityManager.createQuery(
                "select a from Account a", Account.class)
            .getResultList();

            assertEquals( 2, accounts.size());
            //end::pc-filter-entity-query-example[]
        } );

        doInJPA( this::entityManagerFactory, entityManager -> {
            //tag::pc-no-filter-collection-query-example[]
            Client client = entityManager.find( Client.class, 1L );

            assertEquals( 3, client.getAccounts().size() );
            //end::pc-no-filter-collection-query-example[]
        } );

        doInJPA( this::entityManagerFactory, entityManager -> {
            log.infof( "Activate filter [%s]", "activeAccount");

            //tag::pc-filter-collection-query-example[]
            entityManager
                .unwrap( Session.class )
                .enableFilter( "activeAccount" )
                .setParameter( "active", true);

            Client client = entityManager.find( Client.class, 1L );

            assertEquals( 2, client.getAccounts().size() );
            //end::pc-filter-collection-query-example[]
        } );
    }

    public enum AccountType {
        DEBIT,
        CREDIT
    }

    //tag::pc-filter-Client-example[]
    @Entity(name = "Client")
    public static class Client {

        @Id
        private Long id;

        private String name;

        @OneToMany(
            mappedBy = "client",
            cascade = CascadeType.ALL
        )
        @Filter(
            name="activeAccount",
            condition="active_status = :active"
        )
        private List<Account> accounts = new ArrayList<>( );

        //Getters and setters omitted for brevity
    //end::pc-filter-Client-example[]
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
    //tag::pc-filter-Client-example[]

        public void addAccount(Account account) {
            account.setClient( this );
            this.accounts.add( account );
        }
    }
    //end::pc-filter-Client-example[]

    //tag::pc-filter-Account-example[]
    @Entity(name = "Account")
    @FilterDef(
        name="activeAccount",
        parameters = @ParamDef(
            name="active",
            type="boolean"
        )
    )
    @Filter(
        name="activeAccount",
        condition="active_status = :active"
    )
    public static class Account {

        @Id
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        private Client client;

        @Column(name = "account_type")
        @Enumerated(EnumType.STRING)
        private AccountType type;

        private Double amount;

        private Double rate;

        @Column(name = "active_status")
        private boolean active;

        //Getters and setters omitted for brevity
    //end::pc-filter-Account-example[]
        public Long getId() {
            return id;
        }

        public Account setId(Long id) {
            this.id = id;
            return this;
        }

        public Client getClient() {
            return client;
        }

        public Account setClient(Client client) {
            this.client = client;
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

        public boolean isActive() {
            return active;
        }

        public Account setActive(boolean active) {
            this.active = active;
            return this;
        }

    //tag::pc-filter-Account-example[]
    }
    //end::pc-filter-Account-example[]
}
