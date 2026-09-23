/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
		 * The Collection element or Map element.
		 */
		ELEMENT,
		/**
		 * The List index or Map key.
		 */
		INDEX,
		/**
		 * The identifier for and idbag.
		 */
		ID;

		public String getName() {
			return switch (this) {
				case ELEMENT -> "{element}";
				case INDEX -> "{index}";
				case ID -> "{collection-id}";
			};
		}

		public static Nature fromNameExact(String name) {
			return switch ( name ) {
				case "{element}" -> ELEMENT;
				case "{index}" -> INDEX;
				case "{collection-id}" -> ID;
				default -> null;
			};
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

			if ( ID.getName().equals( name ) ) {
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

	@Override
	default boolean isReadOnly() {
		return getCollectionAttribute().isReadOnly();
	}
}
