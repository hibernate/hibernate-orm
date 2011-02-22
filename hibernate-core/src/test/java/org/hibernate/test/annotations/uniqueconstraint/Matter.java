package org.hibernate.test.annotations.uniqueconstraint;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@Entity
public class Matter {

    private Long id;

    private Integer weight;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}
