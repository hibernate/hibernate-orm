/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.sql;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.WrapperOptions;

/**
 * Descriptor for the <tt>SQL</tt>/<tt>JDBC</tt> side of a value mapping.
 *
 * @author Steve Ebersole
 */
public interface SqlTypeDescriptor extends Serializable {
	/**
	 * Return the {@linkplain java.sql.Types JDBC type-code} for the column mapped by this type.
	 *
	 * @return The JDBC type-code
	 */
	public int getSqlType();

	public static interface Binder<X> {
		public void bind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException;
	}

	public <X> Binder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor);

	public static interface Extractor<X> {
		public X extract(ResultSet rs, String name, WrapperOptions options) throws SQLException;
	}

	public <X> Extractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor);
}
