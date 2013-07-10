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
 */
package org.hibernate.test.bytecode.enhancement.customer;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
@Entity
@Table(name="O_CUSTOMER")
public class Customer {
    public static final String QUERY_ALL = "Customer.selectAll";
    public static final String QUERY_COUNT = "Customer.count";
    public static final String QUERY_BY_CREDIT = "Customer.selectByCreditLimit";

    public static final String BAD_CREDIT = "BC";

    @Id
    @Column(name="C_ID")
    private int id;

    @Column(name="C_FIRST")
    private String firstName;

    @Column(name="C_LAST")
    private String lastName;

    @Column(name="C_CONTACT")
    private String contact;

    @Column(name="C_CREDIT")
    private String credit;

    @Column(name="C_CREDIT_LIMIT")
    private BigDecimal creditLimit;

    @Column(name="C_SINCE")
    @Temporal(TemporalType.DATE)
    private Calendar since;

    @Column(name="C_BALANCE")
    private BigDecimal balance;

    @Column(name="C_YTD_PAYMENT")
    private BigDecimal ytdPayment;

    @OneToMany(mappedBy="customer", cascade= CascadeType.ALL, fetch= FetchType.EAGER)
    private List<CustomerInventory> customerInventories;

    @Embedded
    @AttributeOverrides(
            {@AttributeOverride(name="street1",column=@Column(name="C_STREET1")),
                    @AttributeOverride(name="street2",column=@Column(name="C_STREET2")),
                    @AttributeOverride(name="city",   column=@Column(name="C_CITY")),
                    @AttributeOverride(name="state",  column=@Column(name="C_STATE")),
                    @AttributeOverride(name="country",column=@Column(name="C_COUNTRY")),
                    @AttributeOverride(name="zip",    column=@Column(name="C_ZIP")),
                    @AttributeOverride(name="phone",  column=@Column(name="C_PHONE"))})
    private Address       address;

    @Version
    @Column(name = "C_VERSION")
    private int version;

    public Customer() {
    }

    public Customer(String first, String last, Address address,
                    String contact, String credit, BigDecimal creditLimit,
                    BigDecimal balance, BigDecimal YtdPayment) {

        this.firstName   = first;
        this.lastName    = last;
        this.address     = address;
        this.contact     = contact;
        this.since       = Calendar.getInstance();
        this.credit      = credit;
        this.creditLimit = creditLimit;
        this.balance     = balance;
        this.ytdPayment  = YtdPayment;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer customerId) {
        this.id = customerId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getCredit() {
        return credit;
    }

    public void setCredit(String credit) {
        this.credit = credit;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public Calendar getSince() {
        return since;
    }

    public void setSince(Calendar since) {
        this.since = since;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void changeBalance(BigDecimal change) {
        setBalance(balance.add(change).setScale(2, BigDecimal.ROUND_DOWN));
    }

    public BigDecimal getYtdPayment() {
        return ytdPayment;
    }

    public void setYtdPayment(BigDecimal ytdPayment) {
        this.ytdPayment = ytdPayment;
    }

    public List<CustomerInventory> getInventories() {
        if (customerInventories == null){
            customerInventories = new ArrayList<CustomerInventory>();
        }
        return customerInventories;
    }

    public CustomerInventory addInventory(String item, int quantity,
                                          BigDecimal totalValue) {

        CustomerInventory inventory = new CustomerInventory(this, item,
                quantity, totalValue);
        getInventories().add(inventory);
        return inventory;
    }

    public int getVersion() {
        return version;
    }

    public boolean hasSufficientCredit(BigDecimal amount) {
        return !BAD_CREDIT.equals(getCredit())
                && creditLimit != null
                && creditLimit.compareTo(amount) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return id == ((Customer) o).id;
    }

    @Override
    public int hashCode() {
        return new Integer(id).hashCode();
    }

    @Override
    public String toString() {
        return this.getFirstName() + " " + this.getLastName();
    }
}
