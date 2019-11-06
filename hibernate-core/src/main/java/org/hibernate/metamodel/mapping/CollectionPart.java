/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.sql.results.spi.Fetchable;

/**
 * @author Steve Ebersole
 */
public interface CollectionPart extends ModelPart, Fetchable {
	enum Nature {
		ELEMENT( "{element}" ),
		INDEX( "{index}" );

		private final String name;

		Nature(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static Nature fromName(String name) {
			if ( ELEMENT.name.equals( name ) ) {
				return ELEMENT;
			}
			else if ( INDEX.name.equals( name ) ) {
				return INDEX;
			}

			throw new IllegalArgumentException(
					"Unrecognized CollectionPart Nature name [" + name
							+ "]; expecting `" + ELEMENT.name + "` or `"
							+ INDEX.name + "`"
			);
		}
	}

	Nature getNature();

	MappingType getPartTypeDescriptor();

	@Override
	default String getPartName() {
		return getNature().getName();
	}
}
