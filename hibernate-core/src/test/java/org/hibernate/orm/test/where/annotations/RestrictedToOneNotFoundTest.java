/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import org.hibernate.FetchNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.engine.internal.FilteredAssociationState;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DomainModel(annotatedClasses = {
		RestrictedToOneNotFoundTest.PlainTarget.class,
		RestrictedToOneNotFoundTest.SqlTarget.class,
		RestrictedToOneNotFoundTest.FilterTarget.class,
		RestrictedToOneNotFoundTest.PlainIgnore.class,
		RestrictedToOneNotFoundTest.PlainException.class,
		RestrictedToOneNotFoundTest.SqlIgnore.class,
		RestrictedToOneNotFoundTest.SqlException.class,
		RestrictedToOneNotFoundTest.FilterIgnore.class,
		RestrictedToOneNotFoundTest.FilterException.class })
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = @Setting(name = "hibernate.cache.use_second_level_cache", value = "false"))
class RestrictedToOneNotFoundTest {
	record Mapping(Class<? extends Owner> type, String restriction, boolean ignore) {
		String table() {
			return "nf_" + type.getSimpleName().toLowerCase();
		}
	}

	enum Load {
		FIND, HQL, FETCH, NATIVE;

		Owner load(SessionImplementor session, Mapping mapping, long id) {
			return switch ( this ) {
				case FIND -> session.find( mapping.type, id );
				case HQL -> session.createQuery( "from " + mapping.type.getSimpleName() + " o where o.id=:id", mapping.type )
						.setParameter( "id", id ).getSingleResult();
				case FETCH -> session.createQuery( "from " + mapping.type.getSimpleName()
						+ " o left join fetch o.fk left join fetch o.joined left join fetch o.selected"
						+ " left join fetch o.selectedJoin where o.id=:id", mapping.type ).setParameter( "id", id ).getSingleResult();
				case NATIVE -> session.createNativeQuery( "select o.*, j.joined_id, s.selected_join_id from " + mapping.table()
						+ " o left join " + mapping.table() + "_join j on j.owner_id=o.id left join "
						+ mapping.table() + "_select_join s on s.owner_id=o.id where o.id=:id", mapping.type )
						.setParameter( "id", id ).getSingleResult();
			};
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(
				new Mapping( PlainIgnore.class, "Plain", true ),
				new Mapping( PlainException.class, "Plain", false ),
				new Mapping( SqlIgnore.class, "Sql", true ),
				new Mapping( SqlException.class, "Sql", false ),
				new Mapping( FilterIgnore.class, "Filter", true ),
				new Mapping( FilterException.class, "Filter", false ) )
				.flatMap( mapping -> Stream.of( false, true ).flatMap( enabled ->
						Stream.of( Load.values() ).map( load -> Arguments.of( mapping, enabled, load ) ) ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	String targetTable(Mapping mapping) {
		return "nf_" + mapping.restriction.toLowerCase() + "_target";
	}

	@ParameterizedTest
	@MethodSource("cases")
	void distinguishNullExcludedAndDangling(Mapping mapping, boolean enabled, Load load, SessionFactoryScope scope) {
		final String table = mapping.table();
		scope.inTransaction( session -> {
			final String target = targetTable( mapping );
			for ( long id = 1; id <= 2; id++ ) {
				session.createNativeMutationQuery( "insert into " + target + " (id, visible) values (:id, :visible)" )
						.setParameter( "id", id )
						.setParameter( "visible", id == 1 ? 1 : 0 )
						.executeUpdate();
			}
			for ( int id = 1; id <= 4; id++ ) {
				final String key = id == 4 ? "null" : id == 3 ? "99" : Integer.toString( id );
				session.createNativeMutationQuery( "insert into " + table
						+ " (id, note, fk_id, selected_id) values (" + id + ", 'original', " + key + ", " + key + ")" ).executeUpdate();
				if ( id != 4 ) {
					for ( String suffix : new String[] { "_join", "_select_join" } ) {
						session.createNativeMutationQuery( "insert into " + table + suffix
								+ " (owner_id, " + (suffix.equals( "_join" ) ? "joined_id" : "selected_join_id") + ") values (" + id + ", " + key + ")" ).executeUpdate();
					}
				}
			}
		} );
		final boolean restricted = mapping.restriction.equals( "Sql" ) || enabled && mapping.restriction.equals( "Filter" );
		for ( long id = 1; id <= 4; id++ ) {
			final long ownerId = id;
			if ( id == 3 && !mapping.ignore && !restricted ) {
				assertThatThrownBy( () -> scope.inTransaction( session -> {
					if ( enabled ) {
						session.enableFilter( "nf_visible" ).setParameter( "visible", 1 );
					}
					load.load( session, mapping, ownerId );
				} ) ).isInstanceOf( FetchNotFoundException.class );
				continue;
			}
			scope.inTransaction( session -> {
				if ( enabled ) {
					session.enableFilter( "nf_visible" ).setParameter( "visible", 1 );
				}
				final var owner = load.load( session, mapping, ownerId );
				final boolean absent = ownerId == 4 || ownerId == 3 || ownerId == 2 && restricted;
				for ( Object target : owner.targets() ) {
					if ( absent ) {
						assertThat( target ).isNull();
					}
					else {
						assertThat( target ).isNotNull();
						assertThat( Hibernate.isInitialized( target ) ).isTrue();
					}
				}
				final var entry = session.getPersistenceContextInternal().getEntry( owner );
				assertThat( FilteredAssociationState.hasFilteredAssociations( entry ) )
						.isEqualTo( restricted && (ownerId == 2 || ownerId == 3) );
				owner.note = "changed";
				session.flush();
				if ( restricted && (ownerId == 2 || ownerId == 3) ) {
					final long key = ownerId == 2 ? 2L : 99L;
					assertThat( session.createNativeQuery( "select fk_id, selected_id from " + table + " where id=:id", Object[].class )
							.setParameter( "id", ownerId ).getSingleResult() ).containsExactly( key, key );
					for ( String suffix : new String[] { "_join", "_select_join" } ) {
						assertThat( session.createNativeQuery( "select " + (suffix.equals( "_join" ) ? "joined_id" : "selected_join_id") + " from " + table + suffix + " where owner_id=:id", Long.class )
								.setParameter( "id", ownerId ).getSingleResult() ).isEqualTo( key );
					}
				}
			} );
		}
	}

	@MappedSuperclass
	abstract static class Owner {
		@Id long id;
		String note;
		abstract Object[] targets();
	}

	@Entity(name = "NfPlainTarget")
	@Table(name = "nf_plain_target")
	static class PlainTarget {
		@Id Long id;
		int visible;
	}

	@Entity(name = "NfSqlTarget")
	@Table(name = "nf_sql_target")
	@SQLRestriction("visible = 1")
	static class SqlTarget {
		@Id Long id;
		int visible;
	}

	@Entity(name = "NfFilterTarget")
	@Table(name = "nf_filter_target")
	@FilterDef(name = "nf_visible", parameters = @ParamDef(name = "visible", type = Integer.class), applyToLoadByKey = true)
	@Filter(name = "nf_visible", condition = "visible = :visible")
	static class FilterTarget {
		@Id Long id;
		int visible;
	}

	@Entity(name = "PlainIgnore")
	@Table(name = "nf_plainignore")
	static class PlainIgnore extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(name = "fk_id")
		PlainTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@JoinTable(name = "nf_plainignore_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		PlainTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "selected_id")
		PlainTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "nf_plainignore_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		PlainTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}

	@Entity(name = "PlainException")
	@Table(name = "nf_plainexception")
	static class PlainException extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(name = "fk_id")
		PlainTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@JoinTable(name = "nf_plainexception_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		PlainTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "selected_id")
		PlainTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "nf_plainexception_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		PlainTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}

	@Entity(name = "SqlIgnore")
	@Table(name = "nf_sqlignore")
	static class SqlIgnore extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(name = "fk_id")
		SqlTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@JoinTable(name = "nf_sqlignore_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		SqlTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "selected_id")
		SqlTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "nf_sqlignore_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		SqlTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}

	@Entity(name = "SqlException")
	@Table(name = "nf_sqlexception")
	static class SqlException extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(name = "fk_id")
		SqlTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@JoinTable(name = "nf_sqlexception_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		SqlTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "selected_id")
		SqlTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "nf_sqlexception_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		SqlTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}

	@Entity(name = "FilterIgnore")
	@Table(name = "nf_filterignore")
	static class FilterIgnore extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(name = "fk_id")
		FilterTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.JOIN)
		@JoinTable(name = "nf_filterignore_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		FilterTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "selected_id")
		FilterTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.IGNORE)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "nf_filterignore_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		FilterTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}

	@Entity(name = "FilterException")
	@Table(name = "nf_filterexception")
	static class FilterException extends Owner {
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(name = "fk_id")
		FilterTarget fk;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.JOIN)
		@JoinTable(name = "nf_filterexception_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "joined_id"))
		FilterTarget joined;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "selected_id")
		FilterTarget selected;
		@ManyToOne(fetch = FetchType.LAZY)
		@NotFound(action = NotFoundAction.EXCEPTION)
		@Fetch(FetchMode.SELECT)
		@JoinTable(name = "nf_filterexception_select_join", joinColumns = @JoinColumn(name = "owner_id"), inverseJoinColumns = @JoinColumn(name = "selected_join_id"))
		FilterTarget selectedJoin;

		@Override
		Object[] targets() {
			return new Object[] { fk, joined, selected, selectedJoin };
		}
	}
}
