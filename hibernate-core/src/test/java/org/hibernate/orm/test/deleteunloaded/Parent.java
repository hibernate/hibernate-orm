package org.hibernate.orm.test.deleteunloaded;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Parent {
    @GeneratedValue
    @Id
    private long id;
    @Version
    private int version;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
    private Set<Child> children = new HashSet<>();

    public Set<Child> getChildren() {
        return children;
    }

    public long getId() {
        return id;
    }
}
