package org.hibernate.orm.test.mapping.javatime.redesign;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;

import static org.assertj.core.api.Assertions.assertThat;

/// Inspects the actual COLUMN primary descriptors, not a generic TIMESTAMP type.
/// Requires an explicit fractional precision parameter on the same SQL type.
/// Switching between fixed-precision types (for example Sybase bigdatetime and
/// datetime) is not evidence that a requested precision of zero is implemented.
///
/// @author Steve Ebersole
final class TemporalCachePrecisionSupport {
	private TemporalCachePrecisionSupport() {
	}

	static Precision inspect(MetadataImplementor metadata, Class<?> entityClass) {
		final var dialect = metadata.getDatabase().getDialect();
		final var registry = metadata.getTypeConfiguration().getDdlTypeRegistry();
		final var entity = metadata.getEntityBinding( entityClass.getName() );
		final var descriptions = new ArrayList<String>();
		boolean supported = true;
		for ( String property : List.of( "adjusted", "unadjusted" ) ) {
			final var component = (Component) entity.getProperty( property ).getValue();
			final var primary = (BasicValue) component.getProperty( "instant" ).getValue();
			final var column = primary.getColumns().get( 0 );
			final var type = primary.resolve().getJdbcMapping();
			final var descriptor = registry.getDescriptor( type.getJdbcType().getDdlTypeCode() );
			final var actualSize = column.getColumnSize( dialect, metadata );
			assertThat( actualSize.getPrecision() ).as( property + " requested precision" ).isZero();
			final var zero = descriptor.getTypeName( actualSize, primary.getType(), registry );
			final var three = descriptor.getTypeName( new Size().setPrecision( 3 )
					.setLength( actualSize.getLength() ).setScale( actualSize.getScale() ), primary.getType(), registry );
			assertThat( column.getSqlType( metadata ) ).isEqualTo( zero );
			supported &= rendersFractionalPrecision( zero, three );
			descriptions.add( property + ": " + zero + " (precision 3: " + three + ")" );
		}
		final var result = new Precision( supported, List.copyOf( descriptions ) );
		System.out.println( "TEMPORAL_CACHE_PRECISION " + dialect.getClass().getSimpleName() + " " + result );
		return result;
	}

	static boolean rendersFractionalPrecision(String zero, String three) {
		// Deliberately recognize only an explicit timestamp precision parameter,
		// retaining suffixes such as WITH TIME ZONE. Unknown renderings stay ineligible.
		return zero.contains( "(0)" ) && three.equals( zero.replace( "(0)", "(3)" ) );
	}

	record Precision(boolean supported, List<String> primaryTypes) {
	}
}
