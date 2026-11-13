package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.*;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.type.descriptor.converter.internal.TemporalAdjustmentConverter;
import org.hibernate.usertype.CompositeUserType;
import static org.assertj.core.api.Assertions.assertThat;

/// Expected declaration results are fixture inputs, never derived from the
/// resolver's own assessment. COLUMN also proves its configured cache execution.
///
/// @author Steve Ebersole
final class TemporalDeclarationAssertions {
	private TemporalDeclarationAssertions() {
	}

	@SuppressWarnings( { "rawtypes", "unchecked" } )
	static void mapping( ContextScope scope, Class<?> entity, String member, boolean column, ZoneId target ) {
		final var value = scope.metadata.getEntityBinding( entity.getName() ).getProperty( member ).getValue();
		if ( column ) {
			assertThat( value ).as( member ).isInstanceOf( Component.class );
			final var component = ( Component ) value;
			assertThat( component.getColumnSpan() ).isEqualTo( 2 );
			final CompositeUserType composite = component.getContextualCompositeUserType();
			assertThat( composite ).isNotNull();
			final var input = OffsetDateTime.parse( "2026-07-01T12:00+02:00" );
			final var expected = target == null ? input : input.atZoneSameInstant( target ).toOffsetDateTime();
			assertThat( composite.assemble( composite.disassemble( input ), null ) ).isEqualTo( expected );
		} else {
			assertThat( value ).as( member ).isInstanceOf( BasicValue.class );
			final var converter = ( ( BasicValue ) value ).resolve().getValueConverter();
			if ( target == null )
				assertThat( converter ).as( member ).isNull();
			else {
				assertThat( converter ).as( member ).isInstanceOf( TemporalAdjustmentConverter.class );
				assertThat( ( ( TemporalAdjustmentConverter<?> ) converter ).getTarget() ).isEqualTo( target );
			}
			assertThat( value.getColumnSpan() ).isEqualTo( 1 );
		}
	}
}
