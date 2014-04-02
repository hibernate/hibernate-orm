/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.spi;

import java.util.List;
import javax.persistence.AccessType;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;

/**
 * Contract responsible for resolving the members that identify the persistent
 * attributes for a given class descriptor representing a managed type.
 *
 * These members (field or method) would be where we look for mapping annotations
 * for the attribute.
 *
 * Additionally, whether the member is a field or method would tell us the default
 * runtime access strategy
 *
 * @author Steve Ebersole
 */
public interface PersistentAttributeMemberResolver {
	/**
	 * Given the ManagedType Java type descriptor and the implicit AccessType
	 * to use, resolve the members that indicate persistent attributes.
	 *
	 * @param managedTypeDescriptor The ManagedType Java type descriptor
	 * @param classLevelAccessType The AccessType determined for the class default
	 * @param localBindingContext The local binding context
	 *
	 * @return The list of "backing members"
	 */
	public List<MemberDescriptor> resolveAttributesMembers(
			JavaTypeDescriptor managedTypeDescriptor,
			AccessType classLevelAccessType,
			LocalBindingContext localBindingContext);
}
