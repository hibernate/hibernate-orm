package org.hibernate.test.lazycache;

import java.util.Date;

import javax.persistence.*;

import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LazyAttributeEntity {

    private Long id;
    private Date date;
    private String string;
    private long longVal;
    private int intVal;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Basic(fetch = FetchType.LAZY)
    @Temporal(TemporalType.TIMESTAMP)
    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    @Basic(fetch = FetchType.LAZY)
    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Basic(fetch = FetchType.LAZY)
    public long getLongVal() {
        return longVal;
    }

    public void setLongVal(long longVal) {
        this.longVal = longVal;
    }

    @Basic(fetch = FetchType.LAZY)
    public int getIntVal() {
        return intVal;
    }

    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

}
