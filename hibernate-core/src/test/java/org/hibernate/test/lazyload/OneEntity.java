package org.hibernate.test.lazyload;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class OneEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "oneEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Child1Entity> oneChildren;

    @OneToMany(mappedBy = "oneEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Child2Entity> twoChildren;

    public Long getId() {
        return id;
    }

    public Set<Child1Entity> getOneChildren() {
        if (oneChildren == null) {
            oneChildren = new HashSet<>();
        }
        return oneChildren;
    }

    public Set<Child2Entity> getTwoChildren() {
        if (twoChildren == null) {
            twoChildren = new HashSet<>();
        }
        return twoChildren;
    }
}
