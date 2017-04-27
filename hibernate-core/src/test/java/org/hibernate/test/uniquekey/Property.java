package org.hibernate.test.uniquekey;

import org.hibernate.annotations.NaturalId;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class Property implements Serializable{

    @Id
    private Integer id;

    @NaturalId
    private Integer code;

    @NaturalId
    private Integer item;

    private String description = "A description ...";

    protected Property(){}

    public Property(Integer id, Integer code, Integer item){
        this.id = id;
        this.code = code;
        this.item = item;
    }


}
