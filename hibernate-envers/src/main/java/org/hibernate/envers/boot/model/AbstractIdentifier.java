/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Base abstract implementation of {@link Identifier}.
 *
 * @author Chris Cranford
 */
public abstract class AbstractIdentifier implements Identifier {

	private final String propertyName;
	private final List<Attribute> attributes;

	public AbstractIdentifier(String propertyName) {
		this.propertyName = propertyName;
		this.attributes = new ArrayList<>();
	}

	@Override
	public void addAttribute(Attribute attribute) {
		this.attributes.add( attribute );
	}

	@Override
	public String getName() {
		return propertyName;
	}

	@Override
	public List<Attribute> getAttributes() {
		return attributes;
	}

}
