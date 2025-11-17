/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.envers.configuration.internal.metadata.ColumnNameIterator;
import org.hibernate.mapping.Selectable;

/**
 * A contract for identifier relations between persisted entities
 *
 * All attributes stored here are stored in their non-key form, see IdMetadataGenerator.
 *
 * Whenever this container is consulted and attributes are requested to be prefixed, a distinction
 * at the call site determines whether attributes here are to be promoted or not.
 *
 * @author Chris Cranford
 */
public class IdentifierRelation implements AttributeContainer {

	private final List<Attribute> attributes;

	public IdentifierRelation() {
		this.attributes = new ArrayList<>();
	}

	@Override
	public void addAttribute(Attribute attribute) {
		this.attributes.add( attribute );
	}

	public List<Attribute> getAttributesPrefixed(
			String prefix,
			Iterator<Selectable> iterator,
			boolean makeKey,
			boolean insertable) {
		return getAttributesPrefixed(prefix, ColumnNameIterator.from( iterator ), makeKey, insertable );
	}

	public List<Attribute> getAttributesPrefixed(
			String prefix,
			ColumnNameIterator columnNameIterator,
			boolean makeKey,
			boolean insertable) {
		List<Attribute> prefixedAttributes = new ArrayList<>();
		for ( Attribute attribute : attributes ) {
			Attribute prefixedAttribute = attribute.deepCopy();

			String name = prefixedAttribute.getName();
			if ( name != null ) {
				prefixedAttribute.setName( prefix + prefixedAttribute.getName() );
			}

			changeNamesInColumns( prefixedAttribute, columnNameIterator );

			if ( makeKey ) {
				if ( prefixedAttribute instanceof Keyable ){
					( (Keyable) prefixedAttribute ).setKey( true );
				}

				// HHH-11463 when cloning a many-to-one to be a key-many-to-one, the FK attribute
				// should be explicitly set to 'none' or added to be 'none' to avoid issues with
				// making references to the main schema.
				if ( prefixedAttribute instanceof ManyToOneAttribute ) {
					final ManyToOneAttribute manyToOne = (ManyToOneAttribute) prefixedAttribute;
					manyToOne.setForeignKey( "none" );
				}
			}

			if ( prefixedAttribute instanceof BasicAttribute ) {
				( (BasicAttribute) prefixedAttribute ).setInsertable( insertable );
			}

			prefixedAttributes.add( prefixedAttribute );
		}
		return prefixedAttributes;
	}

	private static void changeNamesInColumns(Attribute attribute, ColumnNameIterator columnNameIterator) {
		for ( Column column : attribute.getColumns() ) {
			if ( column.getName() != null ) {
				column.setName( columnNameIterator.next() );
			}
		}
	}
}
