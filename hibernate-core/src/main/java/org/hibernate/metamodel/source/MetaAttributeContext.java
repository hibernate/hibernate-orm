/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.metamodel.source;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.metamodel.binding.MetaAttribute;

/**
 * @author Steve Ebersole
 */
public class MetaAttributeContext {
	private final MetaAttributeContext parentContext;
	private final ConcurrentHashMap<String, MetaAttribute> metaAttributeMap = new ConcurrentHashMap<String, MetaAttribute>();

	public MetaAttributeContext() {
		this( null );
	}

	public MetaAttributeContext(MetaAttributeContext parentContext) {
		this.parentContext = parentContext;
	}


	// read contract ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public Iterable<String> getKeys() {
		HashSet<String> keys = new HashSet<String>();
		addKeys( keys );
		return keys;
	}

	private void addKeys(Set<String> keys) {
		keys.addAll( metaAttributeMap.keySet() );
		if ( parentContext != null ) {
			// recursive call
			parentContext.addKeys( keys );
		}
	}

	public Iterable<String> getLocalKeys() {
		return metaAttributeMap.keySet();
	}

	public MetaAttribute getMetaAttribute(String key) {
		MetaAttribute value = getLocalMetaAttribute( key );
		if ( value == null ) {
			// recursive call
			value = parentContext.getMetaAttribute( key );
		}
		return value;
	}

	public MetaAttribute getLocalMetaAttribute(String key) {
		return metaAttributeMap.get( key );
	}


	// write contract ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void add(MetaAttribute metaAttribute) {
		metaAttributeMap.put( metaAttribute.getName(), metaAttribute );
	}

}
