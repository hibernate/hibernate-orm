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
 * in a domain query (e.g. an SQM tree).
 *
 * todo (6.0) : why is this in the `org.hibernate.**sql**.ast.produce` package?  Why not `org.hibernate.query.sqm`?
 *
 * @author Steve Ebersole
 */
public interface ExpressableType<T> extends javax.persistence.metamodel.Type<T>, Writeable {

	// todo (6.0) : also consider changing from extending Writeable to instead asking for the Writeable
	//		probably passing in TypeConfiguration

	/**
	 * The "java type" descriptor
	 */
	JavaTypeDescriptor<T> getJavaTypeDescriptor();
}
