package org.hibernate.test.criteria;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Nikolay Shestakov
 */
@Entity
public class WithPrimitiveLongEntity {
    @Id
    private int id;
    @Basic
    private long longValue;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }
}
