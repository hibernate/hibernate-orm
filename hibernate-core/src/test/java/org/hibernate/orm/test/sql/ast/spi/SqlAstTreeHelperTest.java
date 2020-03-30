package org.hibernate.orm.test.sql.ast.spi;

import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlAstTreeHelperTest {

	@Nested
	class BaseRestrictionIsNull {

		@Test
		void testNull() {
			final Predicate incomingRestriction = Mockito.mock( Predicate.class );
			assertThat( SqlAstTreeHelper.combinePredicates( null, incomingRestriction ) ).isSameAs( incomingRestriction );
		}

	}

	@Nested
	class IncomingIsNull {

		@Test
		void testNull() {
			final Predicate baseRestriction = Mockito.mock( Predicate.class );
			assertThat( SqlAstTreeHelper.combinePredicates( baseRestriction, null ) ).isSameAs( baseRestriction );
		}

	}

	@Nested
	class BothAreNotNull {

		@Test
		void testBaseIsEmptyJunction() {
			final Junction baseRestriction = Mockito.mock( Junction.class );
			Mockito.when( baseRestriction.isEmpty() ).thenReturn( true );
			final Predicate incomingRestriction = Mockito.mock( Predicate.class );
			assertThat( SqlAstTreeHelper.combinePredicates( baseRestriction, incomingRestriction ) ).isSameAs( incomingRestriction );
		}

		@ParameterizedTest
		@EnumSource( Junction.Nature.class )
		void testBaseIsNotEmpty(Junction.Nature nature) {
			final Junction baseRestriction = Mockito.mock( Junction.class );
			Mockito.when( baseRestriction.isEmpty() ).thenReturn( false );
			Mockito.when( baseRestriction.getNature() ).thenReturn( nature );
			final Predicate incomingRestriction = Mockito.mock( Predicate.class );
			final Predicate combinedPredicates = SqlAstTreeHelper.combinePredicates( baseRestriction, incomingRestriction );

			switch ( nature ) {
				case CONJUNCTION:
					assertThat( combinedPredicates ).isSameAs( baseRestriction );
					Mockito.verify( baseRestriction ).add( incomingRestriction );
					break;
				case DISJUNCTION:
					assertThat( combinedPredicates ).isInstanceOf( Junction.class );
					final Junction combinedJunction = (Junction) combinedPredicates;
					assertThat( combinedJunction.getNature() ).isEqualTo( Junction.Nature.CONJUNCTION );
					assertThat( combinedJunction.getPredicates() ).containsExactly( baseRestriction, incomingRestriction );
					break;
				default:
					throw new IllegalArgumentException( "unknown nature: " + nature );
			}
		}

		@Nested
		class BaseIsNonJunction {

			@Test
			void test() {
				final Predicate baseRestriction = Mockito.mock( Predicate.class );
				final Predicate incomingRestriction = Mockito.mock( Predicate.class );
				final Predicate combinedPredicates = SqlAstTreeHelper.combinePredicates( baseRestriction, incomingRestriction );
				assertThat( combinedPredicates ).isInstanceOf( Junction.class );
				final Junction combinedJunction = (Junction) combinedPredicates;
				assertThat( combinedJunction.getNature() ).isEqualTo( Junction.Nature.CONJUNCTION );
				assertThat( combinedJunction.getPredicates() ).containsExactly( baseRestriction, incomingRestriction );
			}
		}
	}

}
