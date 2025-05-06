/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.util.EnumSet;

import org.hibernate.AnnotationException;
import org.hibernate.Incubating;

/**
 * Condition where an attribute indicates multiple {@linkplain AttributeNature natures}
 *
 * @author Steve Ebersole
 */
@Incubating
public class MultipleAttributeNaturesException extends AnnotationException {
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
		String separator = "";
		for ( AttributeNature nature : natures ) {
			buffer.append( separator );
			buffer.append( nature.name() );
			separator = ",";
		}
		return buffer.append( "]" ).toString();
	}
}
