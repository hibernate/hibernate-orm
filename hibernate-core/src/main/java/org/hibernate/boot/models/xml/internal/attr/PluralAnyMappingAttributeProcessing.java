/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.xml.internal.attr;

import org.hibernate.boot.jaxb.mapping.spi.JaxbPluralAnyMappingImpl;
import org.hibernate.boot.models.xml.internal.XmlAnnotationHelper;
import org.hibernate.boot.models.xml.internal.XmlProcessingHelper;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.MutableMemberDetails;

import jakarta.persistence.AccessType;

import static org.hibernate.internal.util.NullnessHelper.coalesce;

/**
 * @author Steve Ebersole
 */
public class PluralAnyMappingAttributeProcessing {

	@SuppressWarnings("UnusedReturnValue")
	public static MutableMemberDetails processPluralAnyMappingAttributes(
			JaxbPluralAnyMappingImpl jaxbHbmManyToAny,
			MutableClassDetails declarer,
			AccessType classAccessType,
			XmlDocumentContext xmlDocumentContext) {
		final AccessType accessType = coalesce( jaxbHbmManyToAny.getAccess(), classAccessType );
		final MutableMemberDetails memberDetails = XmlProcessingHelper.getAttributeMember(
				jaxbHbmManyToAny.getName(),
				accessType,
				declarer
		);
		XmlAnnotationHelper.applyCascading( jaxbHbmManyToAny.getCascade(), memberDetails, xmlDocumentContext );

		throw new UnsupportedOperationException( "Support for many-to-any attributes not yet implemented" );
	}
}
