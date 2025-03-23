/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.quote;

import java.util.List;

/**
 * @author Brett Meyer
 */
public class AssociatedDataPoint {
	private long id;

	private AssociatedDataPoint manyToOne;

	private List<AssociatedDataPoint> manyToMany;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public AssociatedDataPoint getManyToOne() {
		return manyToOne;
	}

	public void setManyToOne(AssociatedDataPoint manyToOne) {
		this.manyToOne = manyToOne;
	}

	public List<AssociatedDataPoint> getManyToMany() {
		return manyToMany;
	}

	public void setManyToMany(List<AssociatedDataPoint> manyToMany) {
		this.manyToMany = manyToMany;
	}
}
