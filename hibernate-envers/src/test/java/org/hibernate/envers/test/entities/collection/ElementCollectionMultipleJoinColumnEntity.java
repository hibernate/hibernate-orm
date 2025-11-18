package org.hibernate.envers.test.entities.collection;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Audited
@Entity
@Table(name = "foo")
public class ElementCollectionMultipleJoinColumnEntity implements Serializable {
    @Id
    @Column(name = "foo_id")
    private String fooId;

    @ElementCollection
    @Column(name = "bar_id")
    @CollectionTable(
            name = "foos_bars_mapping",
            joinColumns = {
                    @JoinColumn(name = "bazId", referencedColumnName = "baz_id"),
                    @JoinColumn(name = "fooId", referencedColumnName = "foo_id")
            }
    )
    private Set<String> barIds;

    @Column(name = "baz_id")
    private String bazId;

    public String getFooId() {
        return fooId;
    }

    public void setFooId(String fooId) {
        this.fooId = fooId;
    }

    public Set<String> getBarIds() {
        return barIds;
    }

    public void setBarIds(Set<String> barIds) {
        this.barIds = barIds;
    }

    public String getBazId() {
        return bazId;
    }

    public void setBazId(String bazId) {
        this.bazId = bazId;
    }
}
