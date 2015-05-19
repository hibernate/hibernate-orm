/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.quote;

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
