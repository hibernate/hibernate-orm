/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */package org.hibernate.test.bytecode.enhancement.customer;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */

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
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;

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
