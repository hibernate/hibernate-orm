/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import java.io.BufferedWriter;
import java.io.IOException;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;

/**
 * @author Steve Ebersole
 */
public class AttributeSingular extends AttributeSupport {

	public AttributeSingular(MetamodelClass metamodelClass, String name, Class javaType) {
		super( metamodelClass, name, javaType );
	}

	@Override
	public Class<? extends Attribute> getAttributeDescriptorJavaType() {
		return SingularAttribute.class;
	}

	@Override
	public void renderAttributeType(BufferedWriter writer) throws IOException  {
		// JPA stuff already imported
		writer.write(
				format(
						"SingularAttributeSingularAttribute<%s,%s>",
						getOwnerDomainClassName(),
						getAttributeJavaType().getName()
				)
		);
	}
}
