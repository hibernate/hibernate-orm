package org.hibernate.test.uniquekey;

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class Entit implements Serializable{

    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name="PROP_CODE", referencedColumnName = "CODE"),
            @JoinColumn(name="PROP_ITEM", referencedColumnName = "ITEM")
    })
    private Property prop;

    private String severalOtherFields = "Several other fields ...";

    protected Entit() {}

    public Entit(Integer id, Property prop) {
        this.id = id;
        this.prop = prop;
    }

}
