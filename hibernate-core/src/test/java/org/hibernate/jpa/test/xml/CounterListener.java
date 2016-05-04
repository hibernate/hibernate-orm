/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.xml;

import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
public class CounterListener {
	public static int insert;
	public static int update;
	public static int delete;

	@PrePersist
	public void increaseInsert(Object object) {
		insert++;
	}

	@PreUpdate
	public void increaseUpdate(Object object) {
		update++;
	}

	@PreRemove
	public void increaseDelete(Object object) {
		delete++;
	}

	public static void reset() {
		insert = 0;
		update = 0;
		delete = 0;
	}
}
