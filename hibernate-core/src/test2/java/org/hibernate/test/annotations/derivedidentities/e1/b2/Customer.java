/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e1.b2;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

@SuppressWarnings("serial")
@NamedQueries({
   @NamedQuery(name=Customer.QUERY_ALL,
         query="select a from Customer a"),
         @NamedQuery(name=Customer.QUERY_COUNT,
               query="select COUNT(a) from Customer a"),
               @NamedQuery(name=Customer.QUERY_BY_CREDIT,
                     query="SELECT c.id FROM Customer c WHERE c.creditLimit > :limit")
})
@Entity 
@Table(name="O_CUSTOMER") 
public class Customer implements Serializable {
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

   @OneToMany(targetEntity=CustomerInventory.class, mappedBy="customer", cascade=CascadeType.ALL, fetch=FetchType.EAGER)
   private List<CustomerInventory> customerInventories;


   @Version 
   @Column(name = "C_VERSION")
   private int version;

   protected Customer() {
   }

   public Customer(String first, String last, 
         String contact, String credit, BigDecimal creditLimit,
         BigDecimal balance, BigDecimal YtdPayment) {

      this.firstName   = first;
      this.lastName    = last;
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

   public CustomerInventory addInventory(Item item, int quantity, 
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
