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
package org.hibernate.tuple.component;

import org.hibernate.tuple.EntityModeToTuplizerMapping;
import org.hibernate.tuple.Tuplizer;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.EntityMode;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.Serializable;

/**
 * Handles mapping {@link EntityMode}s to {@link ComponentTuplizer}s.
 * <p/>
 * Most of the handling is really in the super class; here we just create
 * the tuplizers and add them to the superclass
 *
 * @author Steve Ebersole
 */
class ComponentEntityModeToTuplizerMapping extends EntityModeToTuplizerMapping implements Serializable {

	// todo : move this to SF per HHH-3517; also see HHH-1907 and ComponentMetamodel
	private ComponentTuplizerFactory componentTuplizerFactory = new ComponentTuplizerFactory();

	public ComponentEntityModeToTuplizerMapping(Component component) {
		PersistentClass owner = component.getOwner();

		// create our own copy of the user-supplied tuplizer impl map
		Map userSuppliedTuplizerImpls = new HashMap();
		if ( component.getTuplizerMap() != null ) {
			userSuppliedTuplizerImpls.putAll( component.getTuplizerMap() );
		}

		// Build the dynamic-map tuplizer...
		Tuplizer dynamicMapTuplizer;
		String tuplizerClassName = ( String ) userSuppliedTuplizerImpls.remove( EntityMode.MAP );
		if ( tuplizerClassName == null ) {
			dynamicMapTuplizer = componentTuplizerFactory.constructDefaultTuplizer( EntityMode.MAP, component );
		}
		else {
			dynamicMapTuplizer = componentTuplizerFactory.constructTuplizer( tuplizerClassName, component );
		}

		// then the pojo tuplizer, using the dynamic-map tuplizer if no pojo representation is available
		tuplizerClassName = ( String ) userSuppliedTuplizerImpls.remove( EntityMode.POJO );
		Tuplizer pojoTuplizer;
		if ( owner.hasPojoRepresentation() && component.hasPojoRepresentation() ) {
			if ( tuplizerClassName == null ) {
				pojoTuplizer = componentTuplizerFactory.constructDefaultTuplizer( EntityMode.POJO, component );
			}
			else {
				pojoTuplizer = componentTuplizerFactory.constructTuplizer( tuplizerClassName, component );
			}
		}
		else {
			pojoTuplizer = dynamicMapTuplizer;
		}

		// then dom4j tuplizer, if dom4j representation is available
		Tuplizer dom4jTuplizer;
		tuplizerClassName = ( String ) userSuppliedTuplizerImpls.remove( EntityMode.DOM4J );
		if ( owner.hasDom4jRepresentation() ) {
			if ( tuplizerClassName == null ) {
				dom4jTuplizer = componentTuplizerFactory.constructDefaultTuplizer( EntityMode.DOM4J, component );
			}
			else {
				dom4jTuplizer = componentTuplizerFactory.constructTuplizer( tuplizerClassName, component );
			}
		}
		else {
			dom4jTuplizer = null;
		}

		// put the "standard" tuplizers into the tuplizer map first
		if ( pojoTuplizer != null ) {
			addTuplizer( EntityMode.POJO, pojoTuplizer );
		}
		if ( dynamicMapTuplizer != null ) {
			addTuplizer( EntityMode.MAP, dynamicMapTuplizer );
		}
		if ( dom4jTuplizer != null ) {
			addTuplizer( EntityMode.DOM4J, dom4jTuplizer );
		}

		// then handle any user-defined entity modes...
		if ( !userSuppliedTuplizerImpls.isEmpty() ) {
			Iterator itr = userSuppliedTuplizerImpls.entrySet().iterator();
			while ( itr.hasNext() ) {
				final Map.Entry entry = ( Map.Entry ) itr.next();
				final EntityMode entityMode = ( EntityMode ) entry.getKey();
				final String userTuplizerClassName = ( String ) entry.getValue();
				ComponentTuplizer tuplizer = componentTuplizerFactory.constructTuplizer( userTuplizerClassName, component );
				addTuplizer( entityMode, tuplizer );
			}
		}
	}
}
