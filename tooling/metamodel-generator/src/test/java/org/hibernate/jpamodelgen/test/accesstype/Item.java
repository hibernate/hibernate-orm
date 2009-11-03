// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jpamodelgen.test.accesstype;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.jpamodelgen.test.accesstype.Product;

/**
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@Entity  
public class Item {
	
	long _id;
	
	int _quantity;
	
	Product _product;
	
	Order _order;

	Detail detail;

	@Id
	public long getId() {
		return _id;
	}

	public void setId(long id) {
		this._id = id;
	}

	public int getQuantity() {
		return _quantity;
	}

	public void setQuantity(int quantity) {
		this._quantity = quantity;
	}

	@ManyToOne
	public Product getProduct() {
		return _product;
	}

	public void setProduct(Product product) {
		this._product = product;
	}

	@ManyToOne
	public Order getOrder() {
		return _order;
	}

	public void setOrder(Order order) {
		this._order = order;
	}
	
	@OneToMany
	public Map<String, Order> getNamedOrders() {
		return null;
	}

	public Detail getDetail() {
		return detail;
	}

	public void setDetail(Detail detail) {
		this.detail = detail;
	}
}
