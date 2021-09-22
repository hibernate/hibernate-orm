package org.hibernate.orm.test.discriminatedcollections;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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

