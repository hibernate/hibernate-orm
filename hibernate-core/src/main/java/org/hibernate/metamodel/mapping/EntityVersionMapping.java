/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.VersionValue;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;

/**
 * Describes the mapping of an entity version
 *
 * @author Steve Ebersole
 */
public interface EntityVersionMapping extends BasicValuedModelPart {
	BasicAttributeMapping getVersionAttribute();

	VersionValue getUnsavedStrategy();
}
