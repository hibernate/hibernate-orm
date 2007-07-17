//$Id: MapLazyInitializer.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.proxy.map;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.proxy.AbstractLazyInitializer;

/**
 * Lazy initializer for "dynamic-map" entity representations.
 *
 * @author Gavin King
 */
public class MapLazyInitializer extends AbstractLazyInitializer implements Serializable {

	MapLazyInitializer(String entityName, Serializable id, SessionImplementor session) {
		super(entityName, id, session);
	}

	public Map getMap() {
		return (Map) getImplementation();
	}

	public Class getPersistentClass() {
		throw new UnsupportedOperationException("dynamic-map entity representation");
	}

}
