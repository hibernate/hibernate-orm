/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.spi;

/**
 * This is a special marker interface designed to represent the union of several traits:
 * - ManagedEntity
 * - Managed
 * - PersistentAttributeInterceptable
 * - SelfDirtinessTracker
 * The need for such a "union" isn't natural in the Java language, but represents a technicality
 * we need to bypass performance issues caused by https://bugs.openjdk.org/browse/JDK-8180450
 * @see org.hibernate.engine.internal.ManagedTypeHelper
 * @see org.hibernate.engine.spi.Managed
 * @see org.hibernate.engine.spi.ManagedEntity
 * @see PersistentAttributeInterceptable
 * @see SelfDirtinessTracker
 */
public interface EnhancedEntity extends ManagedEntity, PersistentAttributeInterceptable, SelfDirtinessTracker {

	//TODO what about ExtendedSelfDirtinessTracker ?
	//TODO CompositeTracker, ManagedMappedSuperclass

}
