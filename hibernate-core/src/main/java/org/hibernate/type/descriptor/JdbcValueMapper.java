/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public interface JdbcValueMapper<X> extends Serializable {
	ValueExtractor<X> getValueExtractor();

	ValueBinder<X> getValueBinder();

	BasicJavaDescriptor<X> getJavaTypeDescriptor();

	SqlTypeDescriptor getSqlTypeDescriptor();
}

