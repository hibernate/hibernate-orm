package org.hibernate.test.annotations.filter.subclass.joined;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@FilterDef(name="ignoreSome", parameters={@ParamDef(name="name", type="string")})
@Filter(name="ignoreSome", condition=":name <> ANIMAL_NAME")
public abstract class Named {

    @Column(name="ANIMAL_NAME")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
