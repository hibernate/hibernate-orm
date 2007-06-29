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
