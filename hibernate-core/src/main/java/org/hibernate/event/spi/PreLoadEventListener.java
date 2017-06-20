/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.io.Serializable;

/**
 * Called before injecting property values into a newly
 * loaded entity instance.
 *
 * @author Gavin King
 */
public interface PreLoadEventListener extends Serializable {
	public void onPreLoad(PreLoadEvent event);
}
