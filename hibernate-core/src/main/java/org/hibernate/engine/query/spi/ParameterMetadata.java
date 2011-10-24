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
package org.hibernate.engine.query.spi;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.QueryParameterException;
import org.hibernate.type.Type;

/**
 * Encapsulates metadata about parameters encountered within a query.
 *
 * @author Steve Ebersole
 */
public class ParameterMetadata implements Serializable {

	private static final OrdinalParameterDescriptor[] EMPTY_ORDINALS = new OrdinalParameterDescriptor[0];

	private final OrdinalParameterDescriptor[] ordinalDescriptors;
	private final Map namedDescriptorMap;

	/**
	 * Instantiates a ParameterMetadata container.
	 *
	 * @param ordinalDescriptors
	 * @param namedDescriptorMap
	 */
	public ParameterMetadata(OrdinalParameterDescriptor[] ordinalDescriptors, Map namedDescriptorMap) {
		if ( ordinalDescriptors == null ) {
			this.ordinalDescriptors = EMPTY_ORDINALS;
		}
		else {
			OrdinalParameterDescriptor[] copy = new OrdinalParameterDescriptor[ ordinalDescriptors.length ];
			System.arraycopy( ordinalDescriptors, 0, copy, 0, ordinalDescriptors.length );
			this.ordinalDescriptors = copy;
		}
		if ( namedDescriptorMap == null ) {
			this.namedDescriptorMap = java.util.Collections.EMPTY_MAP;
		}
		else {
			int size = ( int ) ( ( namedDescriptorMap.size() / .75 ) + 1 );
			Map copy = new HashMap( size );
			copy.putAll( namedDescriptorMap );
			this.namedDescriptorMap = java.util.Collections.unmodifiableMap( copy );
		}
	}

	public int getOrdinalParameterCount() {
		return ordinalDescriptors.length;
	}

	public OrdinalParameterDescriptor getOrdinalParameterDescriptor(int position) {
		if ( position < 1 || position > ordinalDescriptors.length ) {
			String error = "Position beyond number of declared ordinal parameters. " +
					"Remember that ordinal parameters are 1-based! Position: " + position;
			throw new QueryParameterException( error );
		}
		return ordinalDescriptors[position - 1];
	}

	public Type getOrdinalParameterExpectedType(int position) {
		return getOrdinalParameterDescriptor( position ).getExpectedType();
	}

	public int getOrdinalParameterSourceLocation(int position) {
		return getOrdinalParameterDescriptor( position ).getSourceLocation();
	}

	public Set getNamedParameterNames() {
		return namedDescriptorMap.keySet();
	}

	public NamedParameterDescriptor getNamedParameterDescriptor(String name) {
		NamedParameterDescriptor meta = ( NamedParameterDescriptor ) namedDescriptorMap.get( name );
		if ( meta == null ) {
			throw new QueryParameterException( "could not locate named parameter [" + name + "]" );
		}
		return meta;
	}

	public Type getNamedParameterExpectedType(String name) {
		return getNamedParameterDescriptor( name ).getExpectedType();
	}

	public int[] getNamedParameterSourceLocations(String name) {
		return getNamedParameterDescriptor( name ).getSourceLocations();
	}

}
