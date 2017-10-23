/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: SubMulti.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.List;

public class SubMulti extends Multi {
	private float amount;
	private SubMulti parent;
	private List children;
	private List moreChildren;
	/**
	 * Returns the amount.
	 * @return float
	 */
	public float getAmount() {
		return amount;
	}
	
	/**
	 * Sets the amount.
	 * @param amount The amount to set
	 */
	public void setAmount(float amount) {
		this.amount = amount;
	}
	
	/**
	 * Returns the childen.
	 * @return List
	 */
	public List getChildren() {
		return children;
	}
	
	/**
	 * Returns the parent.
	 * @return SubMulti
	 */
	public SubMulti getParent() {
		return parent;
	}
	
	/**
	 * Sets the childen.
	 * @param childen The childen to set
	 */
	public void setChildren(List children) {
		this.children = children;
	}
	
	/**
	 * Sets the parent.
	 * @param parent The parent to set
	 */
	public void setParent(SubMulti parent) {
		this.parent = parent;
	}
	
	/**
	 * Returns the moreChildren.
	 * @return List
	 */
	public List getMoreChildren() {
		return moreChildren;
	}

	/**
	 * Sets the moreChildren.
	 * @param moreChildren The moreChildren to set
	 */
	public void setMoreChildren(List moreChildren) {
		this.moreChildren = moreChildren;
	}

}






