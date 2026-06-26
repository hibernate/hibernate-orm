/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import org.hibernate.MappingException;

import java.util.EnumSet;

/// Condition where an attribute indicates multiple {@linkplain AttributeNature natures}
///
/// @author Steve Ebersole
public class MultipleAttributeNaturesException extends MappingException {
	private final String attributeName;

	public MultipleAttributeNaturesException(
			String attributeName,
			EnumSet<AttributeNature> natures) {
		super( craftMessage( attributeName, natures ) );
		this.attributeName = attributeName;
	}

	public String getAttributeName() {
		return attributeName;
	}

	private static String craftMessage(String attributeName, EnumSet<AttributeNature> natures) {
		final StringBuilder buffer = new StringBuilder( "Attribute `" )
				.append( attributeName )
				.append( "` expressed multiple natures [" );
		natures.forEach( buffer::append );
		return buffer.append( "]" ).toString();
	}
}
