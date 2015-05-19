/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

import org.hibernate.internal.util.xml.XMLHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

import org.dom4j.Element;

/**
 * Performs "instantiation" based on DOM4J elements.
 */
public class Dom4jInstantiator implements Instantiator {
	private final String nodeName;
	private final HashSet isInstanceNodeNames = new HashSet();

	public Dom4jInstantiator(Component component) {
		this.nodeName = component.getNodeName();
		isInstanceNodeNames.add( nodeName );
	}

	public Dom4jInstantiator(PersistentClass mappingInfo) {
		this.nodeName = mappingInfo.getNodeName();
		isInstanceNodeNames.add( nodeName );

		if ( mappingInfo.hasSubclasses() ) {
			Iterator itr = mappingInfo.getSubclassClosureIterator();
			while ( itr.hasNext() ) {
				final PersistentClass subclassInfo = ( PersistentClass ) itr.next();
				isInstanceNodeNames.add( subclassInfo.getNodeName() );
			}
		}
	}
	
	public Object instantiate(Serializable id) {
		return instantiate();
	}
	
	public Object instantiate() {
		return XMLHelper.generateDom4jElement( nodeName );
	}

	public boolean isInstance(Object object) {
		if ( object instanceof Element ) {
			return isInstanceNodeNames.contains( ( ( Element ) object ).getName() );
		}
		else {
			return false;
		}
	}
}
