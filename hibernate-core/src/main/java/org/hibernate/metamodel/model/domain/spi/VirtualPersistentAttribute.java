/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

/**
 * Marker interface indicating that a AttributeDescriptor represents a
 * virtual attribute (aka, not a real attribute in the domain model).
 *
 * @author Steve Ebersole
 */
public interface VirtualPersistentAttribute<O,J> extends PersistentAttributeDescriptor<O,J>, VirtualNavigable<J> {
}
