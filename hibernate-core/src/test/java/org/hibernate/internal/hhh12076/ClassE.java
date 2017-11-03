package org.hibernate.internal.hhh12076;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ClassE {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String name;

    @OneToOne
    @JoinColumn(name = "parent_id")
    private ClassA parent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
