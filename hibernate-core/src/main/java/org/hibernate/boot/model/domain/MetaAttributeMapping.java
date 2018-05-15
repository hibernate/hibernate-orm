/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain;

import java.util.List;

/**
 * Common contract for all things that can handle meta attributes.
 *
 * @author Chris Cranford
 */
public interface MetaAttributeMapping {
	String getName();

	List getValues();

	void addValue(String value);

	String getValue();

	boolean isMultiValued();
}
