/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.entity.customer;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

@SuppressWarnings("serial")
@Entity
@Table(name="O_CUSTINVENTORY")
@IdClass(CustomerInventoryPK.class)
public class CustomerInventory implements Serializable, Comparator<CustomerInventory> {

        public static final String QUERY_COUNT = "CustomerInventory.count";

        @Id
        @TableGenerator(name="inventory",
            table="U_SEQUENCES",
            pkColumnName="S_ID",
            valueColumnName="S_NEXTNUM",
            pkColumnValue="inventory",
            allocationSize=1000)
    @GeneratedValue(strategy= GenerationType.TABLE,generator="inventory")
    @Column(name="CI_ID")
    private Long         id;

    @Id
    @Column(name = "CI_CUSTOMERID", insertable = false, updatable = false)
    private int             custId;

    @ManyToOne(cascade= CascadeType.MERGE)
    @JoinColumn(name="CI_CUSTOMERID")
    private Customer        customer;

    @ManyToOne(cascade=CascadeType.MERGE)
    @JoinColumn(name = "CI_ITEMID")
    private String            vehicle;

    @Column(name="CI_VALUE")
    private BigDecimal totalCost;

    @Column(name="CI_QUANTITY")
    private int             quantity;

    @Version
    @Column(name = "CI_VERSION")
    private int             version;

    public CustomerInventory() {
    }

        CustomerInventory(Customer customer, String vehicle, int quantity,
                      BigDecimal totalValue) {
        this.customer = customer;
        this.vehicle = vehicle;
        this.quantity = quantity;
        this.totalCost = totalValue;
    }

    public String getVehicle() {
        return vehicle;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public int getQuantity() {
        return quantity;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public int getCustId() {
        return custId;
    }

    public int getVersion() {
        return version;
    }

    public int compare(CustomerInventory cdb1, CustomerInventory cdb2) {
        return cdb1.id.compareTo(cdb2.id);
    }

            @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || !(obj instanceof CustomerInventory))
            return false;
        if (this.id == ((CustomerInventory)obj).id)
            return true;
        if (this.id != null && ((CustomerInventory)obj).id == null)
            return false;
        if (this.id == null && ((CustomerInventory)obj).id != null)
            return false;

        return this.id.equals(((CustomerInventory)obj).id);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + custId;
        return result;
    }

}
