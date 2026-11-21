package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.boot.models.annotations.internal.ForeignKeyJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.PrimaryKeyJoinColumnJpaAnnotation;

import org.junit.jupiter.api.Test;

import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;

import static jakarta.persistence.ConstraintMode.CONSTRAINT;
import static jakarta.persistence.ConstraintMode.NO_CONSTRAINT;
import static org.assertj.core.api.Assertions.assertThat;

class PrimaryKeyJoinColumnForeignKeyResolverTest {
	@Test
	void synthesizedContainerDefaultDoesNotHideNestedName() {
		final var container = new ForeignKeyJpaAnnotation( null );
		final var nested = new ForeignKeyJpaAnnotation( null );
		nested.name( "FK_NESTED" );

		assertThat( container.value() ).isEqualTo( CONSTRAINT );
		assertThat( resolve( container, nested ) ).isSameAs( nested );
	}

	@Test
	void scansPastSynthesizedColumnDefault() {
		final var nested = new ForeignKeyJpaAnnotation( null );
		nested.name( "FK_SECOND_COLUMN" );

		assertThat( resolve( new ForeignKeyJpaAnnotation( null ), new ForeignKeyJpaAnnotation( null ), nested ) )
				.isSameAs( nested );
	}

	@Test
	void findsUnnamedDefinitionAndOptions() {
		final var definition = new ForeignKeyJpaAnnotation( null );
		definition.foreignKeyDefinition( "foreign key (id) references target" );
		final var options = new ForeignKeyJpaAnnotation( null );
		options.options( "on delete cascade" );

		assertThat( resolve( new ForeignKeyJpaAnnotation( null ), new ForeignKeyJpaAnnotation( null ), definition ) )
				.isSameAs( definition );
		assertThat( resolve( new ForeignKeyJpaAnnotation( null ), new ForeignKeyJpaAnnotation( null ), options ) )
				.isSameAs( options );
	}

	@Test
	void findsNestedNoConstraint() {
		final var nested = new ForeignKeyJpaAnnotation( null );
		nested.value( NO_CONSTRAINT );

		assertThat( resolve( new ForeignKeyJpaAnnotation( null ), new ForeignKeyJpaAnnotation( null ), nested ) )
				.isSameAs( nested );
	}

	@Test
	void explicitContainerNameTakesPrecedence() {
		final var container = new ForeignKeyJpaAnnotation( null );
		container.name( "FK_CONTAINER" );
		final var nested = new ForeignKeyJpaAnnotation( null );
		nested.name( "FK_NESTED" );

		assertThat( resolve( container, nested ) ).isSameAs( container );
	}

	@Test
	void containerNoConstraintTakesPrecedenceOverNestedName() {
		final var container = new ForeignKeyJpaAnnotation( null );
		container.value( NO_CONSTRAINT );
		final var nested = new ForeignKeyJpaAnnotation( null );
		nested.name( "FK_NESTED" );

		assertThat( resolve( container, nested ) ).isSameAs( container );
	}

	private static ForeignKey resolve(ForeignKey container, ForeignKey... foreignKeys) {
		final var columns = new PrimaryKeyJoinColumn[foreignKeys.length];
		for ( int i = 0; i < foreignKeys.length; i++ ) {
			// The map constructor needs no ModelsContext for this FK-only resolver test.
			columns[i] = new PrimaryKeyJoinColumnJpaAnnotation( Map.of( "foreignKey", foreignKeys[i] ), null );
		}
		return BinderHelper.resolveContainerForeignKey( container, columns );
	}
}
