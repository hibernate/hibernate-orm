/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;

public class AttributeBag extends AttributeSupport {
	private final Class<?> elementJavaType;

	public AttributeBag(
			MetamodelClass metamodelClass,
			String attributeName,
			Class<?> elementJavaType) {
		super( metamodelClass, attributeName, Collection.class );
		this.elementJavaType = elementJavaType;
	}

	@Override
	public void renderAttributeType(BufferedWriter writer) throws IOException {
		writer.write(
				format(
						"CollectionAttribute<%s,%s>",
						getOwnerDomainClassName(),
						elementJavaType.getName()
				)
		);
	}
}
