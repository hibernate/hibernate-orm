/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cfg.persister;

import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Palmtree extends Tree {
	private double leaveSize;

	public double getLeaveSize() {
		return leaveSize;
	}

	public void setLeaveSize(double leaveSize) {
		this.leaveSize = leaveSize;
	}
}
