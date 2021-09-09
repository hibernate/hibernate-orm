package org.hibernate.orm.test.discriminatedcollections;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorColumn(name = "account_type")
abstract class Account {

    @Id
    private Integer id;
    private double amount;
    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    Account() {}

    Account(Integer id, Client client) {
        this.id = id;
        this.client = client;
        amount = 0.0;
        rate = 12.0;
    }

    public Integer getId() {
        return id;
    }
    public Client getClient() {
        return client;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    private Double rate;

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public abstract AccountType getType();
}

@Entity
@DiscriminatorValue("D")
class DebitAccount extends Account {

    public DebitAccount() {
    }

    public DebitAccount(Integer id, Client client) {
        super( id, client );
    }

    @Override
    public AccountType getType() {
        return AccountType.DEBIT;
    }
}

@Entity
@DiscriminatorValue("C")
class CreditAccount extends Account {

    public CreditAccount() {
    }

    public CreditAccount(Integer id, Client client) {
        super( id, client );
    }

    @Override
    public AccountType getType() {
        return AccountType.CREDIT;
    }
}