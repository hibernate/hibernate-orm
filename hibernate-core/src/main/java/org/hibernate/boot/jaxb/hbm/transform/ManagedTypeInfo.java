/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/**
 * Common information between {@linkplain PersistentClass} and {@linkplain Component}
 * used while transforming {@code hbm.xml}
 *
 * @author Steve Ebersole
 */
public class ManagedTypeInfo {
	private final Table table;
	private final Map<String, PropertyInfo> propertyInfoMap = new HashMap<>();

	/**
	 *
	 */
	public ManagedTypeInfo(Table table) {
		this.table = table;
	}

	public Table table() {
		return table;
	}

	public Map<String, PropertyInfo> propertyInfoMap() {
		return propertyInfoMap;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( obj == null || obj.getClass() != this.getClass() ) {
			return false;
		}
		var that = (ManagedTypeInfo) obj;
		return Objects.equals( this.table, that.table ) &&
				Objects.equals( this.propertyInfoMap, that.propertyInfoMap );
	}

	@Override
	public int hashCode() {
		return Objects.hash( table, propertyInfoMap );
	}

	@Override
	public String toString() {
		return "ManagedTypeInfo[" +
				"table=" + table + ", " +
				"propertyInfoMap=" + propertyInfoMap + ']';
	}

}
