/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import java.util.HashSet;
import java.util.Set;

public class OrderContact {

  private int contactId = 0;
  private Set<Order> orders = new HashSet<Order>();
  
  private String contact;

  
  public int getContactId() {
    return contactId;
  }

  public Set<Order> getOrders() {
    return orders;
  }  

  public String getContact() {
    return contact;
  }

  public void setContact(String contact) {
    this.contact = contact;
  }
  
  public String toString() {
    return "[" + getContactId() + ":" + getContact() + "]";
  }
}