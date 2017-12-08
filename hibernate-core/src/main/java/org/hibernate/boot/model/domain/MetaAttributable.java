/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain;

import java.util.Map;

/**
 * @author Chris Cranford
 */
public interface MetaAttributable<T extends MetaAttributeMapping> {
	Map<String, T> getMetaAttributes();

	void setMetaAttributes(Map<String, T> metaAttributes);

	T getMetaAttribute(String name);
}
