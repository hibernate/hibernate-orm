/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.callbacks;

import jakarta.persistence.PostLoad;

/**
 * @author Steve Ebersole
 */
public class Listener2 {
	@PostLoad
	public void wasLoaded(Object entity) {}
}
