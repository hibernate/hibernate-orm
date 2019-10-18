/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * Marker interface for parts of the application domain model that are virtual - do not
 * actually exist in the model classes
 *
 * @author Steve Ebersole
 */
public interface VirtualModelPart extends ModelPart {
}
