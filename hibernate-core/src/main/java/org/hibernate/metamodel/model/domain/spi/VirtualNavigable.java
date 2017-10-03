/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

/**
 * Marker interface indicating that a particular Navigable
 * represents a value not modeled as part of the application's
 * domain model.
 *
 * This is generally values like discriminator, ROW_ID, Hibernate
 * "back-refs", etc.
 *
 * @author Steve Ebersole
 */
public interface VirtualNavigable<J> extends Navigable<J> {
}
