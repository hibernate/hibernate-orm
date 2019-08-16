/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Parts of the ModelPart hierarchy that are type descriptors, as opposed to attributes e.g.
 *
 * @author Steve Ebersole
 */
public interface MappingType extends ModelPart {
	JavaTypeDescriptor getMappedJavaTypeDescriptor();
}
