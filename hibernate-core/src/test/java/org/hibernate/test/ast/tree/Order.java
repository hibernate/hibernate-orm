/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.ast.tree;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

@Entity
public class Order {
  @Id
  private Integer id;

  @OneToMany( fetch = FetchType.LAZY, mappedBy = "order" )
  @OrderBy
  private Set<OrderItem> orderItems;

  @ManyToOne( fetch = FetchType.LAZY )
  private Shipment shipment;
  
  public Order(Integer id, Shipment shipment) {
    this.id = id;
    this.shipment = shipment;
    this.orderItems = new HashSet<>();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Set<OrderItem> getOrderItems() {
    return this.orderItems;
  }

  public void setOrderItems(Set<OrderItem> orderItems) {
    this.orderItems = orderItems;
  }

  public Shipment getShipment() {
    return this.shipment;
  }

  public void setShipment(Shipment shipment) {
    this.shipment = shipment;
  }
}
