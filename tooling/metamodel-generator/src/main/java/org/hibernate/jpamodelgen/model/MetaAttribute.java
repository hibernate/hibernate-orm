/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.model;

import java.util.List;

/**
 * @author Hardy Ferentschik
 */
public interface MetaAttribute {
	String META_ATTRIBUTE_NAME_SUFFIX = "_";

	List<String> getDeclarationStrings();

	String getMetaType();

	String getPropertyName();

	String getTypeDeclaration();

	MetaEntity getHostingEntity();
}
