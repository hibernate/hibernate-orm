/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;

/**
 * Mapping for an entity's natural-id, if one is defined
 */
public interface NaturalIdMapping extends VirtualModelPart {
	String PART_NAME = "{natural-id}";

	/**
	 * The attribute(s) making up the natural-id.
	 */
	List<SingularAttributeMapping> getNaturalIdAttributes();

	@Override
	default String getPartName() {
		return PART_NAME;
	}

	NaturalIdLoader getNaturalIdLoader();
	MultiNaturalIdLoader getMultiNaturalIdLoader();
}
