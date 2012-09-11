/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.spi.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public interface PluralAttribute extends Attribute {
	public String getRole();
	public Nature getNature();
	public Type getElementType();
	public void setElementType(Type elementType);

	/**
	 * Identifies the specific semantic of a plural valued attribute.
	 *
	 * @author Steve Ebersole
	 */
	enum Nature {
		ARRAY( "array", Object[].class ),
		BAG( "bag", Collection.class ),
		IDBAG( "idbag", Collection.class ),
		SET( "set", Set.class ),
		LIST( "list", List.class ),
		MAP( "map", Map.class );

		private final String name;
		private final Class javaContract;
		private final boolean indexable;

		Nature(String name, Class javaContract) {
			this.name = name;
			this.javaContract = javaContract;
			this.indexable = Map.class.isAssignableFrom( javaContract )
					|| List.class.isAssignableFrom( javaContract )
					|| javaContract.isArray();
		}

		public String getName() {
			return name;
		}

		public Class getJavaContract() {
			return javaContract;
		}

		public boolean isIndexable() {
			return indexable;
		}
	}
}
