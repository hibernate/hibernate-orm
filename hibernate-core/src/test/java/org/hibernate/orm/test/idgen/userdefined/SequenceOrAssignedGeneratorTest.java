/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.idgen.userdefined;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;

import org.hibernate.HibernateException;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		SequenceOrAssignedGeneratorTest.MyEntity.class,
		SequenceOrAssignedGeneratorTest.MyVersionedEntity.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class SequenceOrAssignedGeneratorTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from MyEntity" ).executeUpdate();
			session.createMutationQuery( "delete from MyVersionedEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testPersistExistingId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyEntity e = new MyEntity();
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isNotNull();
		} );
	}

	@Test
	public void testPersistNullId(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyEntity e = new MyEntity();
			e.setId( 123L );
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isEqualTo( 123L );
		} );
	}

	@Test
	public void testPersistExistingIdAndVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyVersionedEntity e = new MyVersionedEntity();
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isNotNull();
		} );
	}

	@Test
	public void testPersistNullIdAndVersion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MyVersionedEntity e = new MyVersionedEntity();
			e.setId( 123L );
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isEqualTo( 123L );
		} );
	}

	@Test
	public void testMergeExistingId(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final MyEntity myEntity = scope.fromTransaction( session -> {
			final MyEntity e = new MyEntity();
			e.setId( 124L );
			e.setName( "entity_1" );
			session.persist( e );
			session.flush();
			return e;
		} );
		scope.inTransaction( session -> {
			assertThat( session.contains( myEntity ) ).isFalse();
			myEntity.setName( "merged_entity_1" );
			inspector.clear();
			session.merge( myEntity );
			session.flush();
			assertThat( myEntity.getName() ).isEqualTo( "merged_entity_1" );
			inspector.assertExecutedCount( 2 );
			inspector.assertIsSelect( 0 );
			inspector.assertIsUpdate( 1 );
		} );
	}

	@Test
	public void testMergeNullId(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final MyEntity myEntity = scope.fromTransaction( session -> {
			final MyEntity e = new MyEntity();
			e.setName( "entity_2" );
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isNotNull();
			return e;
		} );
		scope.inTransaction( session -> {
			assertThat( session.contains( myEntity ) ).isFalse();
			myEntity.setName( "merged_entity_2" );
			inspector.clear();
			session.merge( myEntity );
			session.flush();
			assertThat( myEntity.getName() ).isEqualTo( "merged_entity_2" );
			inspector.assertExecutedCount( 2 );
			inspector.assertIsSelect( 0 );
			inspector.assertIsUpdate( 1 );
		} );
	}

	@Test
	public void testMergeExistingIdAndVersion(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final MyVersionedEntity myEntity = scope.fromTransaction( session -> {
			final MyVersionedEntity e = new MyVersionedEntity();
			e.setId( 124L );
			e.setName( "v_entity_1" );
			session.persist( e );
			session.flush();
			return e;
		} );
		scope.inTransaction( session -> {
			assertThat( session.contains( myEntity ) ).isFalse();
			myEntity.setName( "v_merged_entity_1" );
			inspector.clear();
			session.merge( myEntity );
			session.flush();
			assertThat( myEntity.getName() ).isEqualTo( "v_merged_entity_1" );
			inspector.assertExecutedCount( 2 );
			inspector.assertIsSelect( 0 );
			inspector.assertIsUpdate( 1 );
		} );
	}

	@Test
	public void testMergeNullIdAndVersion(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final MyVersionedEntity myEntity = scope.fromTransaction( session -> {
			final MyVersionedEntity e = new MyVersionedEntity();
			e.setName( "v_entity_2" );
			session.persist( e );
			session.flush();
			assertThat( e.getId() ).isNotNull();
			return e;
		} );
		scope.inTransaction( session -> {
			assertThat( session.contains( myEntity ) ).isFalse();
			myEntity.setName( "v_merged_entity_2" );
			inspector.clear();
			session.merge( myEntity );
			session.flush();
			assertThat( myEntity.getName() ).isEqualTo( "v_merged_entity_2" );
			inspector.assertExecutedCount( 2 );
			inspector.assertIsSelect( 0 );
			inspector.assertIsUpdate( 1 );
		} );
	}

	@Entity( name = "MyEntity" )
	public static class MyEntity {
		protected static final String SEQUENCE = "SEQ_MyEntity";

		@Id
		@SequenceOrAssigned
		private Long id;

		private String name;

		public MyEntity() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "MyVersionedEntity" )
	public static class MyVersionedEntity {
		@Id
		@AssignedOrConstant
		private Long id;

		@Version
		private Long version;

		private String name;

		public MyVersionedEntity() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@IdGeneratorType( SequenceOrAssignedGenerator.class )
	@Target( { METHOD, FIELD } )
	@Retention( RUNTIME )
	public @interface SequenceOrAssigned {
	}

	public static class SequenceOrAssignedGenerator extends SequenceStyleGenerator {
		@Override
		public Object generate(SharedSessionContractImplementor session, Object owner) throws HibernateException {
			final Long id;
			if ( owner instanceof MyEntity ) {
				id = ( (MyEntity) owner ).getId();
			}
			else {
				id = null;
			}

			return id != null ? id : super.generate( session, owner );
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}
	}

	@IdGeneratorType( AssignedOrCountGenerator.class )
	@Target( { METHOD, FIELD } )
	@Retention( RUNTIME )
	public @interface AssignedOrConstant {
	}

	public static class AssignedOrCountGenerator implements BeforeExecutionGenerator {
		private Long count;

		public AssignedOrCountGenerator() {
			this.count = 1L;
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			final Long id;
			if ( owner instanceof MyVersionedEntity ) {
				id = ( (MyVersionedEntity) owner ).getId();
			}
			else {
				id = null;
			}
			return id != null ? id : count++;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}
	}
}
