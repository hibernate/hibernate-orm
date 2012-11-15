package org.hibernate.jpa.test.collection;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Child {

    private Integer id;
    private Parent daddy;
    
    public Child() {
        
    }
    
    @Id
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    @ManyToOne
    public Parent getDaddy() {
        return daddy;
    }
    public void setDaddy(Parent daddy) {
        this.daddy = daddy;
    }
    
    
}


