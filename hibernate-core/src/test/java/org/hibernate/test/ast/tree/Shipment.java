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
import javax.persistence.OneToMany;

@Entity
public class Shipment {
  @Id
  private Integer id;

  @OneToMany( fetch = FetchType.LAZY, mappedBy = "shipment" )
  private Set<Order> orders;
  
  public Shipment(Integer id) {
    this.id = id;
    this.orders = new HashSet<>();
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Set<Order> getOrders() {
    return this.orders;
  }

  public void setOrderItems(Set<Order> orders) {
    this.orders = orders;
  }
}
