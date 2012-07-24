package org.hibernate.test.annotations.any;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;

@Entity
@Table(name = "long_property")
public class LongProperty implements Property {
    private Integer id;

    private String name;
    @Column(name = "`value`")
    private Long value;

    public LongProperty() {
        super();
    }

    public LongProperty(String name, Long value) {
        super();
        this.name = name;
        this.value = value;
    }

    public String asString() {
        return Long.toString(value);
    }

    public String getName() {
        return name;
    }

    @Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

}
