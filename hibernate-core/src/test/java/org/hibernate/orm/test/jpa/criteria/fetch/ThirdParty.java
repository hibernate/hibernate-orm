package org.hibernate.orm.test.jpa.criteria.fetch;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class ThirdParty implements Serializable {

    private Integer id;
    private String name;
    private Agent agent;
    private BillingParty billingParty;

    @Id
    @GeneratedValue
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent", nullable = true)
    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billingParty", nullable = true)
    public BillingParty getBillingParty() {
        return billingParty;
    }

    public void setBillingParty(BillingParty billingParty) {
        this.billingParty = billingParty;
    }
}
