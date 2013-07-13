/*
  * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
  * party contributors as indicated by the @author tags or express 
  * copyright attribution statements applied by the authors.  
  * All third-party contributions are distributed under license by 
  * Red Hat, Inc.
  *
  * This copyrighted material is made available to anyone wishing to 
  * use, modify, copy, or redistribute it subject to the terms and 
  * conditions of the GNU Lesser General Public License, as published 
  * by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of 
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public 
  * License along with this distribution; if not, write to:
  * 
  * Free Software Foundation, Inc.
  * 51 Franklin Street, Fifth Floor
  * Boston, MA  02110-1301  USA
  */

package org.hibernate.test.annotations.inheritance.joined;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * @author Sharath Reddy
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "Customer")
@SecondaryTable(name = "CustomerDetails")
public class Customer extends LegalEntity {

	public String customerName;
	public String customerCode;

	@Column
	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String val) {
		this.customerName = val;
	}

	@Column(table="CustomerDetails")
	public String getCustomerCode() {
		return customerCode;
	}

	public void setCustomerCode(String val) {
		this.customerCode = val;
	}
}
