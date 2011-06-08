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
package org.hibernate.metamodel.binding;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A meta attribute is a named value or values.
 * 
 * @author Gavin King
 */
public class MetaAttribute implements Serializable {
	private final String name;
	private List<String> values = new ArrayList<String>();

	public MetaAttribute(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}	

	public List<String> getValues() {
		return Collections.unmodifiableList(values);
	}

	public void addValue(String value) {
		values.add( value );
	}

	public String getValue() {
		if ( values.size() != 1 ) {
			throw new IllegalStateException( "no unique value" );
		}
		return values.get( 0 );
	}

	public boolean isMultiValued() {
		return values.size()>1;
	}

	public String toString() {
		return "[" + name + "=" + values + "]";
	}
}
