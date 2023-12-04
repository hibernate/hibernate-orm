/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.lifecycle;

/**
 * @author Marco Belladelli
 */
public class SimpleEntityListener {
	public void postPersist(Object entity) {
		// used by hibernate lifecycle callbacks
	}

	public void postPersist() {
		// should not be used by hibernate lifecycle callbacks
	}

	public void postPersist(Object arg1, Object arg2) {
		// should not be used by hibernate lifecycle callbacks
	}

	public void postRemove(Object entity) {
		// used by hibernate lifecycle callbacks
	}

	public void postUpdate(Object entity) {
		// used by hibernate lifecycle callbacks
	}

	public void postLoad(Object entity) {
		// used by hibernate lifecycle callbacks
	}
}
