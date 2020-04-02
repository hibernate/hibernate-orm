/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.index.jpa;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author <a href="mailto:stliu@hibernate.org">Strong Liu</a>
 */
@Embeddable
public class Dealer implements Serializable {
	private String name;
	private long rate;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getRate() {
		return rate;
	}

	public void setRate(long rate) {
		this.rate = rate;
	}
}
