/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.criteria;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Order {

  private int orderId;

  public int getOrderId() {
    return orderId;
  }

  private Set<OrderLine> orderLines = new HashSet<OrderLine>();

  public Set<OrderLine> getLines() {
    return Collections.unmodifiableSet(orderLines);
  }

  public void addLine(OrderLine orderLine){
    orderLine.setOrder(this);
    this.orderLines.add(orderLine);
  }
  
  public String toString() {
    return "" + getOrderId() + " - " + getLines();
  }
}
