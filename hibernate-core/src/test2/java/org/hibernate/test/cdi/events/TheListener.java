/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.events;

import javax.persistence.PrePersist;

/**
 * @author Steve Ebersole
 */
public class TheListener {
	private final Monitor monitor;

	@javax.inject.Inject
	public TheListener(Monitor monitor) {
		this.monitor = monitor;
	}

	@PrePersist
	public void onCreate(Object entity) {
		monitor.entitySaved();
	}
}
