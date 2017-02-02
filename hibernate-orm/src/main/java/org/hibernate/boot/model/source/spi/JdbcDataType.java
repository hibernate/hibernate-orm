/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

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
