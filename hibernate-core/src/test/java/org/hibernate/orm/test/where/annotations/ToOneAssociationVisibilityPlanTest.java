/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.ScrollMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { ToOneAssociationVisibilityPlanTest.Target.class,
		ToOneAssociationVisibilityPlanTest.IdOwner.class, ToOneAssociationVisibilityPlanTest.CodeOwner.class })
@SessionFactory
class ToOneAssociationVisibilityPlanTest {
	@BeforeEach
	void prepare(SessionFactoryScope scope) {
		UpperBound.value.set( 8L );
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 8; id++ ) {
				final var target = new Target();
				target.id = id;
				target.code = "code" + id;
				target.segment = (int) (id % 2);
				target.stamp = new Date( id % 2 * 1000 );
				session.persist( target );
				final var idOwner = new IdOwner();
				idOwner.id = id;
				idOwner.target = target;
				session.persist( idOwner );
				final var codeOwner = new CodeOwner();
				codeOwner.id = id;
				codeOwner.target = target;
				session.persist( codeOwner );
			}
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		UpperBound.value.remove();
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void streamRespectsAssociationFilters(boolean uniqueKey, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visibilitySegments" ).setParameterList( "segments", List.of( 0 ) );
			try ( var stream = session.createNativeQuery( sql( uniqueKey ), type( uniqueKey ) ).getResultStream() ) {
				assertThat( stream.peek( owner -> assertThat( owner.target() != null ).isEqualTo( owner.id % 2 == 0 ) ).count() )
						.isEqualTo( 8 );
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void unusedFilterArgumentsDoNotAffectVisibility(boolean uniqueKey, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visibilitySegments" ).setParameterList( "segments", List.of( 0, 1 ) );
			final var unused = session.enableFilter( "visibilityUnused" ).setParameter( "value", 0 );
			try ( var rows = session.createNativeQuery( sql( uniqueKey ), type( uniqueKey ) ).scroll( ScrollMode.FORWARD_ONLY ) ) {
				for ( int id = 1; id <= 8; id++ ) {
					unused.setParameter( "value", id );
					assertThat( rows.next() ).isTrue();
					assertThat( rows.get().target() ).isNotNull();
				}
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void scrollingDetectsMutableScalarsAndResolvedArguments(boolean uniqueKey, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var stamp = new Date( 0 );
			session.enableFilter( "visibilityStamp" ).setParameter( "stamp", stamp );
			session.enableFilter( "visibilityResolvedLimit" );
			try ( var rows = session.createNativeQuery( sql( uniqueKey ), type( uniqueKey ) ).scroll( ScrollMode.FORWARD_ONLY ) ) {
				for ( int id = 1; id <= 8; id++ ) {
					if ( id == 3 ) {
						stamp.setTime( 1000 );
					}
					else if ( id == 5 ) {
						UpperBound.value.set( 4L );
					}
					else if ( id == 7 ) {
						UpperBound.value.set( 8L );
					}
					assertThat( rows.next() ).isTrue();
					assertThat( rows.get().target() != null ).isEqualTo( id == 2 || id == 3 || id == 7 );
				}
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void listExecutionsUseCurrentFilterParameters(boolean uniqueKey, SessionFactoryScope scope) {
		for ( int segment : new int[] { 0, 1, 0 } ) {
			scope.inTransaction( session -> {
				session.enableFilter( "visibilitySegments" ).setParameterList( "segments", List.of( segment ) );
				final var owners = session.createNativeQuery( sql( uniqueKey ), type( uniqueKey ) ).getResultList();
				assertThat( owners ).hasSize( 8 );
				for ( Owner owner : owners ) {
					assertThat( owner.target() != null ).isEqualTo( owner.id % 2 == segment );
				}
			} );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void scrollingDetectsMutatedListsAndFilterEnablement(boolean uniqueKey, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var segments = new ArrayList<>( List.of( 1 ) );
			session.enableFilter( "visibilitySegments" ).setParameterList( "segments", segments );
			try ( var rows = session.createNativeQuery( sql( uniqueKey ), type( uniqueKey ) ).scroll( ScrollMode.FORWARD_ONLY ) ) {
				for ( int id = 1; id <= 8; id++ ) {
					if ( id == 3 ) {
						segments.set( 0, 0 );
					}
					else if ( id == 5 ) {
						segments.add( 1 );
					}
					else if ( id == 7 ) {
						session.enableFilter( "visibilityLimit" ).setParameter( "upper", 6L );
					}
					else if ( id == 8 ) {
						session.disableFilter( "visibilityLimit" );
					}
					assertThat( rows.next() ).isTrue();
					assertThat( rows.get().target() != null ).isEqualTo( segments.contains( id % 2 ) && id != 7 );
				}
				assertThat( rows.next() ).isFalse();
			}
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void scrollingDetectsTargetFiltersAndScalarParameters(boolean uniqueKey, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.enableFilter( "visibilitySegments" ).setParameterList( "segments", List.of( 0, 1 ) );
			final var limit = session.enableFilter( "visibilityLimit" ).setParameter( "upper", 8L );
			try ( var rows = session.createNativeQuery( sql( uniqueKey ), type( uniqueKey ) ).scroll( ScrollMode.FORWARD_ONLY ) ) {
				for ( int id = 1; id <= 8; id++ ) {
					if ( id == 3 ) {
						limit.setParameter( "upper", 2L );
					}
					else if ( id == 5 ) {
						limit.setParameter( "upper", 8L );
						session.enableFilter( "visibilityTarget" );
					}
					else if ( id == 7 ) {
						session.disableFilter( "visibilityTarget" );
					}
					assertThat( rows.next() ).isTrue();
					assertThat( rows.get().target() != null ).isEqualTo( id <= 2 || id == 6 || id >= 7 );
				}
			}
		} );
	}

	private static String sql(boolean uniqueKey) {
		return "select * from " + (uniqueKey ? "visibility_code_owner" : "visibility_id_owner") + " order by id";
	}

	private static Class<? extends Owner> type(boolean uniqueKey) {
		return uniqueKey ? CodeOwner.class : IdOwner.class;
	}

	@Entity(name = "VisibilityTarget")
	@Table(name = "visibility_target")
	@FilterDef(name = "visibilitySegments", defaultCondition = "segment in (:segments)", parameters = @ParamDef(name = "segments", type = Integer.class))
	@FilterDef(name = "visibilityLimit", defaultCondition = "id <= :upper", parameters = @ParamDef(name = "upper", type = Long.class))
	@FilterDef(name = "visibilityTarget", defaultCondition = "segment = 0", applyToLoadByKey = true)
	@FilterDef(name = "visibilityStamp", defaultCondition = "stamp = :stamp", parameters = @ParamDef(name = "stamp", type = Date.class))
	@FilterDef(name = "visibilityResolvedLimit", defaultCondition = "id <= :upper",
			parameters = @ParamDef(name = "upper", type = Long.class, resolver = UpperBound.class))
	@FilterDef(name = "visibilityUnused", parameters = @ParamDef(name = "value", type = Integer.class))
	@Filter(name = "visibilityTarget")
	static class Target {
		@Id Long id;
		@Column(unique = true) String code;
		int segment;
		Date stamp;
	}

	public static class UpperBound implements Supplier<Long> {
		static final ThreadLocal<Long> value = new ThreadLocal<>();
		@Override
		public Long get() {
			return value.get();
		}
	}

	@MappedSuperclass
	abstract static class Owner {
		@Id Long id;
		abstract Target target();
	}

	@Entity(name = "VisibilityIdOwner")
	@Table(name = "visibility_id_owner")
	static class IdOwner extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@SQLRestriction("id > 0")
		@Filter(name = "visibilitySegments")
		@Filter(name = "visibilityLimit")
		@Filter(name = "visibilityStamp")
		@Filter(name = "visibilityResolvedLimit")
		Target target;
		@Override
		Target target() { return target; }
	}

	@Entity(name = "VisibilityCodeOwner")
	@Table(name = "visibility_code_owner")
	static class CodeOwner extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "target_code", referencedColumnName = "code")
		@SQLRestriction("id > 0")
		@Filter(name = "visibilitySegments")
		@Filter(name = "visibilityLimit")
		@Filter(name = "visibilityStamp")
		@Filter(name = "visibilityResolvedLimit")
		Target target;
		@Override
		Target target() { return target; }
	}
}
