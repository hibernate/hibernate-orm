/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Polymorphically represents any  "type" which can occur as an expression
 * in an SQM tree.
 * <p/>
 * Generally will be one of:<ul>
 *     <li>a {@link Navigable}</li>
 *     <li>a {@link org.hibernate.type.spi.BasicType}</li>
 * </ul>
 * <p/>
 * Note: cannot be just Navigable as we need to account for functions
 * and other non-domain expressions.
 *
 * @author Steve Ebersole
 */
public interface ExpressableType<T> extends javax.persistence.metamodel.Type<T> {
	JavaTypeDescriptor<T> getJavaTypeDescriptor();

	// todo (6.0) : need to account for binding and extracting at this level
	//		now encapsulates AttributeConverter, so this is appropriate.
	//
	// 		However	value extraction is a 2-part process:
	//			1) extract the "raw" SQL value - pushed into JdbcValuesSource#getCurrentRowJdbcValues()
	//			2) use ExpressableType to access the "current row JDBC value"


}
