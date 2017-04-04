/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.criteria;

import java.io.Serializable;

import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.Type;

/**
 * @author David Mansfield
 */

interface CriteriaInfoProvider {
	String getName();

	Serializable[] getSpaces();

	PropertyMapping getPropertyMapping();

	Type getType(String relativePath);
}
