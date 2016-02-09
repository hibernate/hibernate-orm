/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

/**
 * JPA ties the notion of {@link SourceDescriptor} and {@link TargetDescriptor}
 * together: meaning that a SourceDescriptor is specific to a given TargetDescriptor.
 * This contract models that association
 *
 * @author Steve Ebersole
 */
public interface JpaTargetAndSourceDescriptor extends SourceDescriptor, TargetDescriptor {
}
