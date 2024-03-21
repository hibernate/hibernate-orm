/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.AccessType;

/**
 * Contract responsible for resolving the members that identify the persistent
 * attributes for a given class descriptor representing a managed type.
 * <p/>
 * These members (field or method) would be where we look for mapping annotations
 * for the attribute.
 * <p/>
 * Additionally, whether the member is a field or method would tell us the default
 * runtime {@linkplain org.hibernate.property.access.spi.PropertyAccessStrategy access strategy}
 *
 * @author Steve Ebersole
 */
public interface PersistentAttributeMemberResolver {
	/**
	 * Given the class descriptor representing a ManagedType and the implicit AccessType
	 * to use, resolve the members that indicate persistent attributes.
	 *
	 * @param classDetails Descriptor of the class
	 * @param classLevelAccessType The implicit AccessType
	 * @param allMemberConsumer Optional callback for each member on the class
	 *
	 * @return The list of "backing members"
	 */
	List<MemberDetails> resolveAttributesMembers(
			ClassDetails classDetails,
			ClassAttributeAccessType classLevelAccessType,
			AllMemberConsumer allMemberConsumer);

}
