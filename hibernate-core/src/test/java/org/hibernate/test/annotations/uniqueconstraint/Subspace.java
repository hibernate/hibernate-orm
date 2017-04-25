package org.hibernate.test.annotations.uniqueconstraint;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
@Entity
public class Subspace extends Space {

    private Long id;

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
