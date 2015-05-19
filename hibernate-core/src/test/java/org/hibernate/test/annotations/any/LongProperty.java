/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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

    @Column(name = "`value`")
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
