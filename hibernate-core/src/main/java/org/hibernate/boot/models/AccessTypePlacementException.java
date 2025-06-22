/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Access;

/**
 * Indicates a problem with the placement of the {@link Access} annotation; either<ul>
 *     <li>{@linkplain jakarta.persistence.AccessType#FIELD FIELD} on a getter</li>
 *     <li>{@linkplain jakarta.persistence.AccessType#PROPERTY PROPERTY} on a field
 *     <li>{@linkplain jakarta.persistence.AccessType#PROPERTY PROPERTY} on a setter</li></li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class AccessTypePlacementException extends MappingException {
	public AccessTypePlacementException(ClassDetails classDetails, MemberDetails memberDetails) {
		super( craftMessage( classDetails, memberDetails ) );
	}

	private static String craftMessage(ClassDetails classDetails, MemberDetails memberDetails) {
		if ( memberDetails.isField() ) {
			return String.format(
					Locale.ROOT,
					"Field `%s.%s` defined `@Access(PROPERTY) - see section 2.3.2 of the specification",
					classDetails.getName(),
					memberDetails.getName()
			);
		}
		else {
			return String.format(
					Locale.ROOT,
					"Method `%s.%s` defined `@Access(FIELD) - see section 2.3.2 of the specification",
					classDetails.getName(),
					memberDetails.getName()
			);
		}

	}
}
