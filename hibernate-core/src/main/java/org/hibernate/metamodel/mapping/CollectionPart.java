/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.descriptor.java.JavaTypedExpressible;

/**
 * Hibernate understands mapping a collection into 4 parts<ol>
 *     <li>The key - the foreign-key defining the association to the owner</li>
 *     <li>The element - for Maps this is analogous to the value</li>
 *     <li>The index - the List/array index or Map key</li>
 *     <li>The collection-id - this is only relevant for id-bag mappings</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
public interface CollectionPart extends ValuedModelPart, Fetchable, JavaTypedExpressible {
	enum Nature {
		/**
		 * The Collection element or Map element
		 */
		ELEMENT( "{element}" ),
		/**
		 * The List index or Map key
		 */
		INDEX( "{index}" ),
		/**
		 * The identifier for
		 */
		ID( "{collection-id}" );

		private final String name;

		Nature(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public static Nature fromNameExact(String name) {
			switch ( name ) {
				case "{element}":
					return ELEMENT;
				case "{index}":
					return INDEX;
				case "{collection-id}":
					return ID;
			}

			return null;
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
	}

	Nature getNature();

	PluralAttributeMapping getCollectionAttribute();

	@Override
	default String getPartName() {
		return getNature().getName();
	}

	default ModelPart getInclusionCheckPart() {
		return this;
	}
}
