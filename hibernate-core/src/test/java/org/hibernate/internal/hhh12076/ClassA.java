package org.hibernate.internal.hhh12076;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ClassA {
    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String name;

    @OneToMany(cascade = CascadeType.ALL)
    private List<ClassB> subClasses = new ArrayList<>();

    @OneToOne(mappedBy = "parent")
    private ClassE additionalClass;

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

    public List<ClassB> getSubClasses() {
        return subClasses;
    }

    public void setSubClasses(List<ClassB> subClasses) {
        this.subClasses = subClasses;
    }

    public ClassE getAdditionalClass() {
        return additionalClass;
    }

    public void setAdditionalClass(ClassE additionalClass) {
        this.additionalClass = additionalClass;
    }
}
