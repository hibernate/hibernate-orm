/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
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
