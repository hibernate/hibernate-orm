/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collections;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.AttributeMappingsMap;

public final class EmptyAttributeMappingsMap implements AttributeMappingsMap {

	public static final EmptyAttributeMappingsMap INSTANCE = new EmptyAttributeMappingsMap();

	@Override
	public void forEachValue(Consumer<? super AttributeMapping> action) {
		//no-op
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public AttributeMapping get(String name) {
		return null;
	}

	@Override
	public Iterable<AttributeMapping> valueIterator() {
		return Collections.EMPTY_LIST;
	}

}
