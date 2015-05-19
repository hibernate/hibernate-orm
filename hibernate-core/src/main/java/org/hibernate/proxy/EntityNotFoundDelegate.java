/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.proxy;
import java.io.Serializable;

/**
 * Delegate to handle the scenario of an entity not found by a specified id.
 *
 * @author Steve Ebersole
 */
public interface EntityNotFoundDelegate {
	public void handleEntityNotFound(String entityName, Serializable id);
}
