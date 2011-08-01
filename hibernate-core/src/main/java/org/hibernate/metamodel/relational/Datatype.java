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
package org.hibernate.metamodel.relational;

/**
 * Models a JDBC {@link java.sql.Types DATATYPE}
 *
 * @todo Do we somehow link this in with {@link org.hibernate.internal.util.jdbc.TypeInfo} ?
 *
 * @author Steve Ebersole
 */
public class Datatype {
	private final int typeCode;
	private final String typeName;
	private final Class javaType;
	private final int hashCode;

	public Datatype(int typeCode, String typeName, Class javaType) {
		this.typeCode = typeCode;
		this.typeName = typeName;
		this.javaType = javaType;
		this.hashCode = generateHashCode();
	}

    private int generateHashCode() {
        int result = typeCode;
        if ( typeName != null ) {
            result = 31 * result + typeName.hashCode();
        }
        if ( javaType != null ) {
            result = 31 * result + javaType.hashCode();
        }
        return result;
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
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		Datatype datatype = (Datatype) o;

		return typeCode == datatype.typeCode
				&& javaType.equals( datatype.javaType )
				&& typeName.equals( datatype.typeName );

	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return super.toString() + "[code=" + typeCode + ", name=" + typeName + ", javaClass=" + javaType.getName() + "]";
	}
}
