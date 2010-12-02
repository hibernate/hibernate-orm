package org.hibernate.envers.test.integration.collection.norevision;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Audited
public class Person implements Serializable {

    private Integer id;
    @AuditMappedBy(mappedBy = "person")
    private Set<Name> names;

    public Person() {
        names = new HashSet<Name>();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Set<Name> getNames() {
        return names;
    }

    public void setNames(Set<Name> names) {
        this.names = names;
    }
}
