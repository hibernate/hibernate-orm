/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.tuple;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;

import org.dom4j.Element;

import org.hibernate.internal.util.xml.XMLHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

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