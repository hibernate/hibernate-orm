/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.sql.results.graph.Fetchable;

/**
 * @author Steve Ebersole
 */
public interface CollectionPart extends ModelPart, Fetchable {
	enum Nature {
		ELEMENT( "{element}" ),
		INDEX( "{index}" ),
		ID( "{collection-id}" );

		private final String name;

		Nature(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static Nature fromName(String name) {
			// NOTE : the `$x$` form comes form order-by handling
			//		todo (6.0) : ^^ convert these to use the `{x}` form instead?

			if ( "key".equals( name ) || "{key}".equals( name )
					|| "keys".equals( name ) || "{keys}".equals( name )
					|| "index".equals( name ) || "{index}".equals( name )
					|| "indices".equals( name ) || "{indices}".equals( name )
					|| "$index$".equals( name )) {
				return INDEX;
			}

			if ( "element".equals( name ) || "{element}".equals( name )
					|| "elements".equals( name ) || "{elements}".equals( name )
					|| "value".equals( name ) || "{value}".equals( name )
					|| "values".equals( name ) || "{values}".equals( name )
					|| "$element$".equals( name ) ) {
				return ELEMENT;
			}

			if ( ID.name.equals( name ) ) {
				return ID;
			}

			return null;
		}

		public static Nature fromNameStrictly(String name) {
			for ( Nature nature : Nature.values() ) {
				if ( nature.getName().equals( name ) ) {
					return nature;
				}
			}
			return null;
		}
	}

	Nature getNature();

	@Override
	default String getPartName() {
		return getNature().getName();
	}
}
