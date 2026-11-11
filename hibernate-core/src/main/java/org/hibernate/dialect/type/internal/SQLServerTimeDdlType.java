package org.hibernate.dialect.type.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static org.hibernate.type.SqlTypes.TIME;

/// SQL Server TIME retains the server default when precision is omitted.
/// An explicit zero must remain distinct from omission, including in casts.
///
/// @author Steve Ebersole
public class SQLServerTimeDdlType extends DdlTypeImpl {
	public SQLServerTimeDdlType(Dialect dialect) {
		super( TIME, "time($p)", "time", dialect );
	}

	public static void validatePrecision(Integer precision) {
		if ( precision != null && (precision < 0 || precision > 7) ) {
			throw new IllegalArgumentException( "SQL Server TIME precision must be between 0 and 7; requested " + precision );
		}
	}

	@Override
	protected String formatTypeName(Long length, Integer precision, Integer scale) {
		validatePrecision( precision );
		return precision == null ? "time" : super.formatTypeName( length, precision, scale );
	}

	@Override
	public String getCastTypeName(Size size, SqlExpressible type, DdlTypeRegistry registry) {
		return formatTypeName( size.getLength(), size.getPrecision(), size.getScale() );
	}
}
