package org.hibernate.orm.test.bytecode.foreignpackage;

import org.hibernate.orm.test.bytecode.SuperclassEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ConcreteEntity extends SuperclassEntity {
    @Id
    protected long id;
    protected String bname;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getBname() {
        return bname;
    }

    public void setBname(String bname) {
        this.bname = bname;
    }
}
