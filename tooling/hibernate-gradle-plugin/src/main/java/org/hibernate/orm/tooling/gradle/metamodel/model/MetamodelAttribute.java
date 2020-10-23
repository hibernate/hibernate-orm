/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle.metamodel.model;

import java.io.BufferedWriter;
import javax.persistence.metamodel.Attribute;

/**
 * @author Steve Ebersole
 */
public interface MetamodelAttribute {
	String getName();

	Class<? extends Attribute> getAttributeDescriptorJavaType();
	Class<?> getAttributeJavaType();

	void renderJpaMembers(BufferedWriter writer);

	void renderNameConstant(BufferedWriter writer);
}
