/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal;

import java.util.List;

import org.hibernate.envers.configuration.internal.metadata.reader.PersistentPropertiesSource;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.ModifierUtils;
import org.hibernate.models.internal.dynamic.DynamicFieldDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;

/**
 * Simple helper class to streamline hibernate-models interactions.
 *
 * @author Marco Belladelli
 */
public class ModelsHelper {
	/**
	 * Provides a list of the provided class' {@link MemberDetails} based on the provided access type.
	 *
	 * @param classDetails The class details to extract the members from
	 * @param accessType The access type to use, accepted values are {@code field}, {@code property} and {@code record}
	 *
	 * @return The list of member details
	 */
	public static List<? extends MemberDetails> getMemberDetails(ClassDetails classDetails, String accessType) {
		return switch ( accessType ) {
			case "field" -> classDetails.getFields();
			case "property" -> classDetails.getMethods();
			case "record" -> classDetails.getRecordComponents();
			default -> throw new IllegalArgumentException( "Unknown access type " + accessType );
		};
	}

	/**
	 * Retrieves the {@link MemberDetails member} of the class, being either the field or the getter,
	 * with the provided name if one exists, {@code null} otherwise.
	 *
	 * @param classDetails The class details containing the desired member
	 * @param name The name of the member to find
	 *
	 * @return The requested member, null if not found
	 */
	public static MemberDetails getMember(ClassDetails classDetails, String name) {
		final FieldDetails field = classDetails.findFieldByName( name );
		if ( field != null ) {
			return field;
		}

		for ( MethodDetails method : classDetails.getMethods() ) {
			if ( method.resolveAttributeName().equals( name ) ) {
				return method;
			}
		}

		return null;
	}

	/**
	 * Instantiates a {@link DynamicFieldDetails} from the provided properties source with the specified name
	 *
	 * @param propertiesSource The property source containing the virtual field
	 * @param propertyName The property name of the dynamic field
	 * @param modelsContext Context object for models
	 *
	 * @return The newly created dynamic field details
	 */
	public static FieldDetails dynamicFieldDetails(
			PersistentPropertiesSource propertiesSource,
			String propertyName,
			ModelsContext modelsContext) {
		return new DynamicFieldDetails(
				propertyName,
				new ClassTypeDetailsImpl( propertiesSource.getClassDetails(), TypeDetails.Kind.CLASS ),
				propertiesSource.getClassDetails(),
				ModifierUtils.DYNAMIC_ATTRIBUTE_MODIFIERS,
				false,
				false,
				modelsContext
		);
	}
}
