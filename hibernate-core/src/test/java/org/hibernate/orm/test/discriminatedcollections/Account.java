package org.hibernate.orm.test.discriminatedcollections;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorColumn(name = "account_type")
abstract class Account {

    Account() {}

    Account(Client client) {
        this.client = client;
        amount = 0.0;
        rate = 12.0;
    }

    @Id @GeneratedValue
    private Long id;

    public Long getId() {
        return id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    private Client client;

    public Client getClient() {
        return client;
    }

    private double amount;

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

    public DebitAccount(Client client) {
        super(client);
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

    public CreditAccount(Client client) {
        super(client);
    }

    @Override
    public AccountType getType() {
        return AccountType.CREDIT;
    }
}