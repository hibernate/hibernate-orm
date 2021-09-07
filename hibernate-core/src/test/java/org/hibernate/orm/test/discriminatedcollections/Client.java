package org.hibernate.orm.test.discriminatedcollections;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

@Entity
class Client {

    @Id @GeneratedValue
    private Long id;

    public Long getId() {
        return id;
    }

    private String name;

    public String getName() {
        return name;
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client", fetch = FetchType.LAZY)
    private Set<DebitAccount> debitAccounts = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client", fetch = FetchType.LAZY)
    private Set<CreditAccount> creditAccounts = new HashSet<>();

    Client() {}

    Client(String name) {
        this.name = name;
    }

    public Set<CreditAccount> getCreditAccounts() {
        return creditAccounts;
    }

    public Set<DebitAccount> getDebitAccounts() {
        return debitAccounts;
    }
}

