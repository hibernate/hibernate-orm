package org.hibernate.vector.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

/**
 * Spanner GoogleSQL DDL type for vector types with support for dimension constraints.
 *
 * @since 7.2
 */
public class SpannerVectorDdlType extends VectorDdlType {

	private final String baseType;

	public SpannerVectorDdlType(int sqlTypeCode, String baseType, Dialect dialect) {
		super( sqlTypeCode, "ARRAY<" + baseType + ">", "ARRAY<" + baseType + ">", dialect );
		this.baseType = baseType;
	}

	public String getBaseType() {
		return baseType;
	}

	@Override
	public String getTypeName(Size size, Type type, DdlTypeRegistry ddlTypeRegistry) {
		final Integer arrayLength = size != null ? size.getArrayLength() : null;
		if ( arrayLength != null && arrayLength > 0 ) {
			return "ARRAY<" + baseType + ">(vector_length=>" + arrayLength + ")";
		}
		return "ARRAY<" + baseType + ">";
	}

	public String getTypeName(Size size) {
		return getTypeName( size, (Type) null, null );
	}

	@Override
	public String getCastTypeName(Size size, SqlExpressible type, DdlTypeRegistry ddlTypeRegistry) {
		return "ARRAY<" + baseType + ">";
	}
}
