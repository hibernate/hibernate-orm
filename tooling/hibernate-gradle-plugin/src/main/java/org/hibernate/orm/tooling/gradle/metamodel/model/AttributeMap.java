/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;

public class AttributeMap extends AttributeSupport {
	private final Class<?> keyJavaType;
	private final Class<?> elementJavaType;

	public AttributeMap(
			MetamodelClass metamodelClass,
			String attributeName,
			Class<?> keyJavaType,
			Class<?> elementJavaType) {
		super( metamodelClass, attributeName, Set.class );
		this.keyJavaType = keyJavaType;
		this.elementJavaType = elementJavaType;
	}

	@Override
	public void renderAttributeType(BufferedWriter writer) throws IOException {
		writer.write(
				format(
						"MapAttribute<%s,%s,%s>",
						getOwnerDomainClassName(),
						keyJavaType.getName(),
						elementJavaType.getName()
				)
		);
	}
}
