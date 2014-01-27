/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.metamodel.spi.relational;

/**
 * Models a JDBC {@link java.sql.Types DATATYPE}.  Mainly breaks down into 3 pieces of information:<ul>
 *     <li>
 *         {@link #getTypeCode() type code} - The JDBC type code; generally matches a code from {@link java.sql.Types}
 *         though not necessarily.
 *     </li>
 *     <li>
 *         {@link #getTypeName() type name} - The database type name for the given type code.
 *     </li>
 *     <li>
 *         {@link #getJavaType()} java type} - The java type recommended for representing this JDBC type (if known)
 *     </li>
 * </ul>
 *
 * @todo Would love to link this in with {@link org.hibernate.engine.jdbc.internal.TypeInfo}
 *
 * @author Steve Ebersole
 */
public class JdbcDataType {
	private final int typeCode;
	private final String typeName;
	private final Class javaType;
	private final int hashCode;

	public JdbcDataType(int typeCode, String typeName, Class javaType) {
		this.typeCode = typeCode;
		this.typeName = typeName;
		this.javaType = javaType;

        int result = typeCode;
        if ( typeName != null ) {
            result = 31 * result + typeName.hashCode();
        }
        if ( javaType != null ) {
            result = 31 * result + javaType.hashCode();
        }
		this.hashCode = result;
	}

    public int getTypeCode() {
		return typeCode;
	}

	public String getTypeName() {
		return typeName;
	}

	public Class getJavaType() {
		return javaType;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		JdbcDataType jdbcDataType = (JdbcDataType) o;

		return typeCode == jdbcDataType.typeCode
				&& javaType.equals( jdbcDataType.javaType )
				&& typeName.equals( jdbcDataType.typeName );

	}

	@Override
	public String toString() {
		return super.toString() + "[code=" + typeCode + ", name=" + typeName + ", javaClass=" + javaType.getName() + "]";
	}
}
