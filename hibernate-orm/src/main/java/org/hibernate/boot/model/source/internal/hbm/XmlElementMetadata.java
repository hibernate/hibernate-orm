/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Locale;

/**
 * Provides meta-information about XML elements.
 *
 * @author Steve Ebersole
 */
public enum XmlElementMetadata {
	/**
	 * Describes the {@code <id/>} element
	 */
	ID( true, true ),
	/**
	 * Describes the {@code <composite-id/>} element
	 */
	COMPOSITE_ID( false, true ),
	/**
	 * Describes the {@code <discriminator/>} element
	 */
	DISCRIMINATOR( true, false ),
	/**
	 * Describes the {@code <multi-tenancy/>} element
	 */
	MULTI_TENANCY( true, false ),
	/**
	 * Describes the {@code <version/>} element
	 */
	VERSION( true, true ),
	/**
	 * Describes the {@code <timestamp/>} element
	 */
	TIMESTAMP( true, true ),
	/**
	 * Describes the {@code <natural-id/>} element
	 */
	NATURAL_ID( false, false ),
	/**
	 * Describes the {@code <properties/>} element
	 */
	PROPERTIES( false, true ),
	/**
	 * Describes the {@code <property/>} element
	 */
	PROPERTY( false, true ),
	/**
	 * Describes the {@code <key-property/>} element
	 */
	KEY_PROPERTY( false, true ),
	/**
	 * Describes the {@code <many-to-one/>} element
	 */
	MANY_TO_ONE( false, true ),
	/**
	 * Describes the {@code <key-many-to-one/>} element
	 */
	KEY_MANY_TO_ONE( false, true ),
	/**
	 * Describes the {@code <one-to-one/>} element
	 */
	ONE_TO_ONE( false, true ),
	/**
	 * Describes the {@code <any/>} element
	 */
	ANY( false, true ),
	/**
	 * Describes the {@code <component/>} element
	 */
	COMPONENT( false, true ),
	/**
	 * Describes the {@code <key/>} element
	 */
	KEY( false, false ),
	/**
	 * Describes the {@code <set/>} element
	 */
	SET( false, true ),
	/**
	 * Describes the {@code <list/>} element
	 */
	LIST( false, true ),
	/**
	 * Describes the {@code <bag/>} element
	 */
	BAG( false, true ),
	/**
	 * Describes the {@code <id-bag/>} element
	 */
	ID_BAG( false, true ),
	/**
	 * Describes the {@code <map/>} element
	 */
	MAP( false, true ),
	/**
	 * Describes the {@code <array/>} element
	 */
	ARRAY( false, true ),
	/**
	 * Describes the {@code <primitive-array/>} element
	 */
	PRIMITIVE_ARRAY( false, true ),
	/**
	 * Describes the {@code <collection-id/>} element
	 */
	COLLECTION_ID( true, false ),
	/**
	 * Describes the {@code <element/>} element
	 */
	ELEMENT( false, false ),
	/**
	 * Describes the {@code <many-to-many/>} element
	 */
	MANY_TO_MANY( false, false ),
	/**
	 * Describes the {@code <many-to-aany/>} element
	 */
	MANY_TO_ANY( false, false ),
	/**
	 * Describes the {@code <map-key/>} element
	 */
	MAP_KEY( false, false ),
	/**
	 * Describes the {@code <map-key-many-to-many/>} element
	 */
	MAP_KEY_MANY_TO_MANY( false, false ),

	/**
	 * Describes the {@code <index/>} element
	 */
	INDEX( false, false ),
	/**
	 * Describes the {@code <index-many-to-many/>} element
	 */
	INDEX_MANY_TO_MANY( false, false ),
	/**
	 * Describes the {@code <list-index/>} element
	 */
	LIST_INDEX( true, false );

	private final boolean inherentlySingleColumn;
	private final boolean canBeNamed;

	XmlElementMetadata(boolean inherentlySingleColumn, boolean canBeNamed) {
		this.inherentlySingleColumn = inherentlySingleColumn;
		this.canBeNamed = canBeNamed;
	}

	/**
	 * The corresponding {@code hbm.xml} element name.  Used in error reporting
	 *
	 * @return The {@code hbm.xml} element name
	 */
	public String getElementName() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Can this source, by nature, define just a single column/formula?
	 *
	 * @return {@code true} indicates that the source will refer to just a
	 * single column.
	 */
	public boolean isInherentlySingleColumn() {
		return inherentlySingleColumn;
	}

	/**
	 * Can the source be named.  This is used in implicit naming (naming strategy).
	 *
	 * @return {@code true} indicates that the source can be named and therefore
	 * the column (assuming just one) is eligible for implicit naming.
	 */
	public boolean canBeNamed() {
		return canBeNamed;
	}
}
