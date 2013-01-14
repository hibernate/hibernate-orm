/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.StringTypeDescriptor;
import org.hibernate.type.descriptor.sql.NVarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link String}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class StringNVarcharType
		extends AbstractSingleColumnStandardBasicType<String>
		implements DiscriminatorType<String> {

	public static final StringNVarcharType INSTANCE = new StringNVarcharType();

	public StringNVarcharType() {
		super( NVarcharTypeDescriptor.INSTANCE, StringTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "nstring";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return false;
	}

	public String objectToSQLString(String value, Dialect dialect) throws Exception {
		return '\'' + value + '\'';
	}

	public String stringToObject(String xml) throws Exception {
		return xml;
	}

	public String toString(String value) {
		return value;
	}
}
