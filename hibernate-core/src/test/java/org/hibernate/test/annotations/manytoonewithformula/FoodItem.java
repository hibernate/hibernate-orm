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

package org.hibernate.test.annotations.manytoonewithformula;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

/**
 * @author Sharath Reddy
 */
@Entity
public class FoodItem {
	private Integer id;
	private String item;
	private Menu order;
	
	@Id @GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getItem() {
		return item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	@ManyToOne
	@JoinColumnsOrFormulas(
	{ 
	  @JoinColumnOrFormula(column=@JoinColumn(name="order_nbr", referencedColumnName="order_nbr")),
	  @JoinColumnOrFormula(formula=@JoinFormula(value="'F'", referencedColumnName="is_default"))
	})
	public Menu getOrder() {
		return order;
	}

	public void setOrder(Menu order) {
		this.order = order;
	}
}
