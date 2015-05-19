/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
