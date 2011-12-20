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

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for the <tt>SQL</tt>/<tt>JDBC</tt> side of a value mapping.
 *
 * @author Steve Ebersole
 */
public interface SqlTypeDescriptor extends Serializable {
	/**
	 * Return the {@linkplain java.sql.Types JDBC type-code} for the column mapped by this type.
	 *
	 * @return typeCode The JDBC type-code
	 */
	public int getSqlType();

	/**
	 * Is this descriptor available for remapping?
	 *
	 * @return {@code true} indicates this descriptor can be remapped; otherwise, {@code false}
	 *
	 * @see org.hibernate.type.descriptor.WrapperOptions#remapSqlTypeDescriptor
	 * @see org.hibernate.dialect.Dialect#remapSqlTypeDescriptor
	 */
	public boolean canBeRemapped();

	public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor);

	public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor);
}
