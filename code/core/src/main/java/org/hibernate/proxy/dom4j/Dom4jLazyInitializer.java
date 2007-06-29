// $Id: Dom4jLazyInitializer.java 9210 2006-02-03 22:15:19Z steveebersole $
package org.hibernate.proxy.dom4j;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.proxy.AbstractLazyInitializer;
import org.dom4j.Element;

import java.io.Serializable;

/**
 * Lazy initializer for "dom4j" entity representations.
 *
 * @author Steve Ebersole
 */
public class Dom4jLazyInitializer extends AbstractLazyInitializer implements Serializable {

	Dom4jLazyInitializer(String entityName, Serializable id, SessionImplementor session) {
		super(entityName, id, session);
	}

	public Element getElement() {
		return (Element) getImplementation();
	}

	public Class getPersistentClass() {
		throw new UnsupportedOperationException("dom4j entity representation");
	}

}
