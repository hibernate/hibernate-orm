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
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.descriptor.sql.CharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CHAR CHAR(1)} and {@link Boolean} (using 'Y' and 'N')
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class YesNoType
		extends AbstractSingleColumnStandardBasicType<Boolean>
		implements PrimitiveType<Boolean>, DiscriminatorType<Boolean> {

	public static final YesNoType INSTANCE = new YesNoType();

	public YesNoType() {
		super( CharTypeDescriptor.INSTANCE, BooleanTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "yes_no";
	}

	public Class getPrimitiveClass() {
		return boolean.class;
	}

	public Boolean stringToObject(String xml) throws Exception {
		return fromString( xml );
	}

	public Serializable getDefaultValue() {
		return Boolean.FALSE;
	}

	@SuppressWarnings({ "UnnecessaryUnboxing" })
	public String objectToSQLString(Boolean value, Dialect dialect) throws Exception {
		return StringType.INSTANCE.objectToSQLString( value.booleanValue() ? "Y" : "N", dialect );
	}
}
