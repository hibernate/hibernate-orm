/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated.delegate;

import org.hibernate.annotations.Generated;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.insert.AbstractReturningDelegate;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		GeneratedWritableDelegateTest.ValuesOnly.class,
		GeneratedWritableDelegateTest.ValuesAndIdentity.class,
} )
@SessionFactory
@RequiresDialect( value = PostgreSQLDialect.class, comment = "To write a trigger only once" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-10921" )
public class GeneratedWritableDelegateTest {
	@Test
	public void testValuesOnly(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		// insert
		scope.inTransaction( session -> {
			final ValuesOnly entity = new ValuesOnly( 1L, "Marco" );
			session.persist( entity );
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "MARCO" );

			final GeneratedValuesMutationDelegate delegate = getDelegate(
					scope,
					ValuesOnly.class,
					MutationType.INSERT
			);
			inspector.assertExecutedCount(
					delegate instanceof AbstractReturningDelegate && delegate.supportsArbitraryValues() ? 1 : 2
			);
		} );
		// update
		scope.inTransaction( session -> {
			final ValuesOnly entity = session.find( ValuesOnly.class, 1L );
			entity.setName( "Andrea" );
			inspector.clear();
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "ANDREA" );

			final GeneratedValuesMutationDelegate delegate = getDelegate(
					scope,
					ValuesOnly.class,
					MutationType.UPDATE
			);
			inspector.assertExecutedCount(
					delegate instanceof AbstractReturningDelegate && delegate.supportsArbitraryValues() ? 1 : 2
			);
		} );
	}

	@Test
	public void testValuesAndIdentity(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		// insert
		final Long id = scope.fromTransaction( session -> {
			final ValuesAndIdentity entity = new ValuesAndIdentity( "Marco" );
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "MARCO" );

			final GeneratedValuesMutationDelegate delegate = getDelegate(
					scope,
					ValuesOnly.class,
					MutationType.INSERT
			);
			inspector.assertExecutedCount(
					delegate instanceof AbstractReturningDelegate && delegate.supportsArbitraryValues() ? 1 : 2
			);
			return entity.getId();
		} );
		// update
		scope.inTransaction( session -> {
			final ValuesAndIdentity entity = session.find( ValuesAndIdentity.class, id );
			entity.setName( "Andrea" );
			inspector.clear();
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "ANDREA" );

			final GeneratedValuesMutationDelegate delegate = getDelegate(
					scope,
					ValuesOnly.class,
					MutationType.UPDATE
			);
			inspector.assertExecutedCount(
					delegate instanceof AbstractReturningDelegate && delegate.supportsArbitraryValues() ? 1 : 2
			);
		} );
	}

	private static GeneratedValuesMutationDelegate getDelegate(
			SessionFactoryScope scope,
			Class<?> entityClass,
			MutationType mutationType) {
		final EntityPersister entityDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( entityClass );
		return entityDescriptor.getMutationDelegate( mutationType );
	}

	private static final String TRIGGER = "$$ begin new.name = upper(new.name); return new; end; $$ language plpgsql;";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createNativeMutationQuery( "create or replace function fun_upper_name() returns trigger as " + TRIGGER )
					.executeUpdate();
			s.createNativeMutationQuery( "drop trigger if exists upper_name_1 on values_only" ).executeUpdate();
			s.createNativeMutationQuery( "drop trigger if exists upper_name_2 on values_and_identity" ).executeUpdate();
			s.createNativeMutationQuery(
					"create trigger upper_name_1 before insert or update on values_only for each row execute procedure fun_upper_name()"
			).executeUpdate();
			s.createNativeMutationQuery(
					"create trigger upper_name_2 before insert or update on values_and_identity for each row execute procedure fun_upper_name()"
			).executeUpdate();
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createNativeMutationQuery( "drop trigger if exists upper_name_1 on values_only" ).executeUpdate();
			s.createNativeMutationQuery( "drop trigger if exists upper_name_2 on values_and_identity" ).executeUpdate();
			s.createNativeMutationQuery( "drop function if exists fun_upper_name()" ).executeUpdate();
			s.createMutationQuery( "delete from ValuesOnly" ).executeUpdate();
			s.createMutationQuery( "delete from ValuesAndIdentity" ).executeUpdate();
		} );
	}

	@Entity( name = "ValuesOnly" )
	@Table( name = "values_only" )
	static class ValuesOnly {
		@Id
		private Long id;

		@Generated( writable = true, event = { INSERT, UPDATE } )
		private String name;

		public ValuesOnly() {
		}

		public ValuesOnly(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "ValuesAndIdentity" )
	@Table( name = "values_and_identity" )
	static class ValuesAndIdentity {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		@Generated( writable = true, event = { INSERT, UPDATE } )
		private String name;

		public ValuesAndIdentity() {
		}

		public ValuesAndIdentity(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
