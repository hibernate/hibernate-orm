package org.hibernate.test.unidir;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "CHILD2")
public class Child2 {
    @Id
    @Column(name = "ID")
    private Long id;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHILD2_ID", nullable = false)
    private List<Parent1> parents = new ArrayList<Parent1>();

    public Long getId() {
        return this.id;
    }

    public List<Parent1> getParents() {
        return this.parents;
    }

    public void setParents(List<Parent1> parents) {
        this.parents = parents;
    }
}
