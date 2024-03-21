/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models;

import java.util.EnumSet;

import org.hibernate.MappingException;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;

/**
 * Condition where an attribute indicates multiple {@linkplain AttributeMetadata.AttributeNature natures}
 *
 * @author Steve Ebersole
 */
public class MultipleAttributeNaturesException extends MappingException {
	private final String attributeName;

	public MultipleAttributeNaturesException(
			String attributeName,
			EnumSet<AttributeMetadata.AttributeNature> natures) {
		super( craftMessage( attributeName, natures ) );
		this.attributeName = attributeName;
	}

	public String getAttributeName() {
		return attributeName;
	}

	private static String craftMessage(String attributeName, EnumSet<AttributeMetadata.AttributeNature> natures) {
		final StringBuilder buffer = new StringBuilder( "Attribute `" )
				.append( attributeName )
				.append( "` expressed multiple natures [" );
		String separator = "";
		for ( AttributeMetadata.AttributeNature nature : natures ) {
			buffer.append( separator );
			buffer.append( nature.name() );
			separator = ",";
		}
		return buffer.append( "]" ).toString();
	}
}
