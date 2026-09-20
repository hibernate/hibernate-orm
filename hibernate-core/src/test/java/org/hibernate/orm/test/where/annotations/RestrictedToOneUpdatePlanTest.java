/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.model.TableUpdate;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.DIALECT;

@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = { RestrictedToOneUpdatePlanTest.Owner.class, RestrictedToOneUpdatePlanTest.Target.class })
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.where.annotations.RestrictedToOneUpdatePlanTest$CountingDialect"))
class RestrictedToOneUpdatePlanTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	private void prepare(SessionFactoryScope scope, int count) {
		scope.inTransaction( session -> {
			final var visible = new Target();
			visible.id = 1L;
			final var hidden = new Target();
			hidden.id = 2L;
			session.persist( visible );
			session.persist( hidden );
			for ( int i = 1; i <= count; i++ ) {
				final var owner = new Owner();
				owner.id = (long) i;
				owner.name = "original";
				owner.note = "original";
				owner.linkNote = "original";
				for ( int bit = 0; bit < 6; bit++ ) {
					owner.setTarget( bit, (i & (1 << bit)) == 0 ? visible : hidden );
				}
				owner.joined = hidden;
				session.persist( owner );
			}
		} );
	}

	private int translations(SessionFactoryScope scope) {
		return ((CountingDialect) scope.getSessionFactory().getJdbcServices().getDialect()).updateTranslations.get();
	}

	private void update(SessionFactoryScope scope, long id, long visibleId, String value, boolean note) {
		scope.inTransaction( session -> {
			session.enableFilter( "plan_visible" ).setParameter( "id", visibleId );
			final var owner = session.find( Owner.class, id );
			if ( note ) {
				owner.note = value;
			}
			else {
				owner.name = value;
			}
			owner.linkNote = value;
		} );
	}

	@Test
	void reuseAcrossSessionsAndDifferentValues(SessionFactoryScope scope) {
		prepare( scope, 2 );
		scope.inTransaction( session -> {
			final var original = session.find( Owner.class, 1L );
			final var copy = new Owner();
			copy.id = 3L;
			copy.name = "original";
			copy.note = "original";
			copy.linkNote = "original";
			for ( int i = 0; i < 6; i++ ) {
				copy.setTarget( i, original.targets()[i] );
			}
			copy.joined = original.joined;
			session.persist( copy );
		} );
		update( scope, 1, 1, "first", false );
		final int first = translations( scope );
		update( scope, 3, 1, "another owner", false );
		assertThat( translations( scope ) ).isEqualTo( first );
		update( scope, 1, 1, "second", false );
		assertThat( translations( scope ) ).isEqualTo( first );
		// Different omission masks must not share a plan.
		update( scope, 2, 1, "third", false );
		final int second = translations( scope );
		update( scope, 2, 1, "fourth", false );
		assertThat( translations( scope ) ).isEqualTo( second );
		// A different dirty attribute may change the assignments, depending on the queue.
		update( scope, 2, 1, "fifth", true );
		final int changedAssignments = translations( scope );
		update( scope, 2, 1, "sixth", true );
		assertThat( translations( scope ) ).isEqualTo( changedAssignments );
		// Changing the filter parameter changes which columns must be preserved.
		update( scope, 2, 2, "seventh", true );
		final int changedFilter = translations( scope );
		update( scope, 2, 2, "eighth", true );
		assertThat( translations( scope ) ).isEqualTo( changedFilter );
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 2L );
			assertThat( owner.name ).isEqualTo( "fourth" );
			assertThat( owner.note ).isEqualTo( "eighth" );
			assertThat( owner.version ).isEqualTo( 6 );
			assertThat( owner.a.id ).isEqualTo( 1L );
			assertThat( owner.b.id ).isEqualTo( 2L );
			assertThat( owner.joined.id ).isEqualTo( 2L );
		} );
	}

	@Test
	void replacementStopsOmittingTheColumn(SessionFactoryScope scope) {
		prepare( scope, 1 );
		update( scope, 1, 1, "warm", false );
		scope.inTransaction( session -> {
			session.enableFilter( "plan_visible" ).setParameter( "id", 1L );
			final var owner = session.find( Owner.class, 1L );
			assertThat( owner.a ).isNull();
			owner.a = session.find( Target.class, 1L );
			owner.name = "replaced";
			session.flush();
			owner.a = null;
			owner.name = "cleared";
			session.flush();
		} );
		scope.inTransaction( session -> {
			final var owner = session.find( Owner.class, 1L );
			assertThat( owner.a ).isNull();
			assertThat( owner.joined.id ).isEqualTo( 2L );
			assertThat( owner.name ).isEqualTo( "cleared" );
			assertThat( owner.version ).isEqualTo( 3 );
		} );
	}

	@Test
	void boundedCacheEvictsAndRegeneratesCorrectly(SessionFactoryScope scope) {
		prepare( scope, 63 );
		for ( int id = 1; id <= 63; id++ ) {
			update( scope, id, 1, "first", false );
		}
		final int warmed = translations( scope );
		for ( int id = 1; id <= 63; id++ ) {
			update( scope, id, 1, "second", false );
		}
		assertThat( translations( scope ) ).isGreaterThan( warmed );
		scope.inTransaction( session -> {
			for ( var owner : session.createQuery( "from PlanOwner", Owner.class ).list() ) {
				assertThat( owner.name ).isEqualTo( "second" );
				assertThat( owner.version ).isEqualTo( 2 );
				for ( int bit = 0; bit < 6; bit++ ) {
					assertThat( owner.targets()[bit].id ).isEqualTo( (owner.id & (1 << bit)) == 0 ? 1L : 2L );
				}
				assertThat( owner.joined.id ).isEqualTo( 2L );
			}
		} );
	}

	public static class CountingDialect extends H2Dialect {
		private final AtomicInteger updateTranslations = new AtomicInteger();

		@Override
		public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
			final var delegate = super.getSqlAstTranslatorFactory();
			return new SqlAstTranslatorFactory() {
				@Override
				public <S extends Statement, O extends JdbcOperation> SqlAstTranslator<O> buildTranslator(
						SqlAstTranslationRequest<S, O> request) {
					if ( request instanceof SqlAstTranslationRequest.ModelMutation<?> mutation
							&& mutation.statement() instanceof TableUpdate<?> ) {
						updateTranslations.incrementAndGet();
					}
					return delegate.buildTranslator( request );
				}
			};
		}
	}

	@Entity(name = "PlanTarget")
	@Table(name = "plan_target")
	@FilterDef(name = "plan_visible", parameters = @ParamDef(name = "id", type = Long.class), applyToLoadByKey = true)
	@Filter(name = "plan_visible", condition = "id = :id")
	static class Target {
		@Id Long id;
	}

	@Entity(name = "PlanOwner")
	@Table(name = "plan_owner")
	static class Owner {
		@Id Long id;
		@Version int version;
		String name;
		String note;
		@ManyToOne @JoinColumn(name = "a_id") Target a;
		@ManyToOne @JoinColumn(name = "b_id") Target b;
		@ManyToOne @JoinColumn(name = "c_id") Target c;
		@ManyToOne @JoinColumn(name = "d_id") Target d;
		@ManyToOne @JoinColumn(name = "e_id") Target e;
		@ManyToOne @JoinColumn(name = "f_id") Target f;
		@ManyToOne @JoinTable(name = "plan_join", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")) Target joined;
		@Column(table = "plan_join") String linkNote;

		Target[] targets() {
			return new Target[] { a, b, c, d, e, f };
		}

		void setTarget(int bit, Target target) {
			switch ( bit ) {
				case 0 -> a = target;
				case 1 -> b = target;
				case 2 -> c = target;
				case 3 -> d = target;
				case 4 -> e = target;
				case 5 -> f = target;
				default -> throw new IllegalArgumentException();
			}
		}
	}
}
