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

    @Id
    private Integer id;

    private String name;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client", fetch = FetchType.LAZY)
    private Set<DebitAccount> debitAccounts = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "client", fetch = FetchType.LAZY)
    private Set<CreditAccount> creditAccounts = new HashSet<>();

    Client() {}

    Client(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<CreditAccount> getCreditAccounts() {
        return creditAccounts;
    }

    public Set<DebitAccount> getDebitAccounts() {
        return debitAccounts;
    }
}

