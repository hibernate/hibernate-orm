/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.list;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Steve Ebersole
 */
public class Order {
	private Integer id;
	private String code;
	private List<LineItem> lineItems = new ArrayList<LineItem>();

	public Order() {
	}

	public Order(String code) {
		this.code = code;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public List<LineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(List<LineItem> lineItems) {
		this.lineItems = lineItems;
	}

	public LineItem addLineItem(String productCode, int quantity) {
		LineItem lineItem = new LineItem( this, productCode, quantity );
		lineItems.add( lineItem );
		return lineItem;
	}
}
