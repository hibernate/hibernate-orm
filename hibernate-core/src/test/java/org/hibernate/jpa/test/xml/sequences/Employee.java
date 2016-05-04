/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $
package org.hibernate.jpa.test.xml.sequences;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Employee {
    @Id
	Long id;
    String name;
/*
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "HA_street")),
        @AttributeOverride(name = "city", column = @Column(name = "HA_city")),
        @AttributeOverride(name = "state", column = @Column(name = "HA_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "HA_zip")) })
*/
    Address homeAddress;

/*
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "MA_street")),
        @AttributeOverride(name = "city", column = @Column(name = "MA_city")),
        @AttributeOverride(name = "state", column = @Column(name = "MA_state")),
        @AttributeOverride(name = "zip", column = @Column(name = "MA_zip")) })
*/
    Address mailAddress;

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

    public Address getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(Address homeAddress) {
        this.homeAddress = homeAddress;
    }

    public Address getMailAddress() {
        return mailAddress;
    }

    public void setMailAddress(Address mailAddress) {
        this.mailAddress = mailAddress;
    }
}