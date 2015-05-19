/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e1.b.specjmapid.lazy;

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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Version;

import org.hibernate.test.annotations.derivedidentities.e1.b.specjmapid.Item;

@NamedQueries({ @NamedQuery(name = "CustomerInventoryTwo.selectAll", query = "select a from CustomerInventoryTwo a") })
@SuppressWarnings("serial")
@Entity
@Table(name = "O_CUSTINVENTORY")
@IdClass(CustomerInventoryTwoPK.class)
public class CustomerInventoryTwo implements Serializable,
      Comparator<CustomerInventoryTwo> {

   @Id
   @TableGenerator(name = "inventory", table = "U_SEQUENCES", pkColumnName = "S_ID", valueColumnName = "S_NEXTNUM", pkColumnValue = "inventory", allocationSize = 1000)
   @GeneratedValue(strategy = GenerationType.TABLE, generator = "inventory")
   @Column(name = "CI_ID")
   private Integer id;

   @Id
   @Column(name = "CI_CUSTOMERID", insertable = false, updatable = false)
   private int custId;

   @ManyToOne(cascade = CascadeType.MERGE)
   @JoinColumn(name = "CI_CUSTOMERID", nullable = false)
   private CustomerTwo customer;

   @ManyToOne(cascade = CascadeType.MERGE)
   @JoinColumn(name = "CI_ITEMID")
   private Item vehicle;

   @Column(name = "CI_VALUE")
   private BigDecimal totalCost;

   @Column(name = "CI_QUANTITY")
   private int quantity;

   @Version
   @Column(name = "CI_VERSION")
   private int version;

   protected CustomerInventoryTwo() {
   }

   CustomerInventoryTwo(CustomerTwo customer, Item vehicle, int quantity,
         BigDecimal totalValue) {
      this.customer = customer;
      this.vehicle = vehicle;
      this.quantity = quantity;
      this.totalCost = totalValue;
   }

   public Item getVehicle() {
      return vehicle;
   }

   public BigDecimal getTotalCost() {
      return totalCost;
   }

   public int getQuantity() {
      return quantity;
   }

   public Integer getId() {
      return id;
   }

   public CustomerTwo getCustomer() {
      return customer;
   }

   public int getCustId() {
      return custId;
   }

   public int getVersion() {
      return version;
   }

   public int compare(CustomerInventoryTwo cdb1, CustomerInventoryTwo cdb2) {
      return cdb1.id.compareTo(cdb2.id);
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj == null || !(obj instanceof CustomerInventoryTwo)) {
         return false;
      }
      if (this.id == ((CustomerInventoryTwo) obj).id) {
         return true;
      }
      if (this.id != null && ((CustomerInventoryTwo) obj).id == null) {
         return false;
      }
      if (this.id == null && ((CustomerInventoryTwo) obj).id != null) {
         return false;
      }

      return this.id.equals(((CustomerInventoryTwo) obj).id);
   }

}
