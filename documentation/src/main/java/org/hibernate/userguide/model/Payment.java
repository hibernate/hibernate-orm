package org.hibernate.userguide.model;

import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.ManyToOne;

/**
 * @author Vlad Mihalcea
 */
//tag::hql-examples-domain-model-example[]
@Entity
@Inheritance
public class Payment {

    @Id
    @GeneratedValue
    private Long id;

    private BigDecimal amount;

    private boolean completed;

    @ManyToOne
    private Person person;

    //Getters and setters are omitted for brevity

//end::hql-examples-domain-model-example[]

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }
//tag::hql-examples-domain-model-example[]
}
//end::hql-examples-domain-model-example[]
