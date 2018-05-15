/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.metamodel.model.domain.spi.Writeable;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Polymorphically represents any  "type" which can occur as an expression
 * in a "domain query" (e.g. an SQM tree).
 *
 * @author Steve Ebersole
 */
public interface ExpressableType<T> extends javax.persistence.metamodel.Type<T>, Writeable {

	/**
	 * The "java type" descriptor
	 */
	JavaTypeDescriptor<T> getJavaTypeDescriptor();
}
