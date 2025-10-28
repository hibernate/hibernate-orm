/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.EnumSet;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.generator.EventTypeSets.INSERT_ONLY;

/**
 * @author Marco Belladelli
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( annotatedClasses = {
		MixedTimingGeneratorsTest.AssignedEntity.class,
		MixedTimingGeneratorsTest.RandomEntity.class,
		MixedTimingGeneratorsTest.StringGeneratedEntity.class,
} )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17322" )
public class MixedTimingGeneratorsTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@SkipForDialect( dialectClass = SQLServerDialect.class, reason = "SQLServer does not support setting explicit values for identity columns" )
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "Oracle does not support setting explicit values for identity columns" )
	@SkipForDialect( dialectClass = SybaseASEDialect.class, reason = "Sybase does not support setting explicit values for identity columns" )
	public void testIdentityOrAssignedId(SessionFactoryScope scope) {
		// on execution generation
		scope.inTransaction( session -> session.persist( new AssignedEntity( "identity" ) ) );
		scope.inSession( session -> assertThat( session.createQuery(
				"from AssignedEntity where name = :name",
				AssignedEntity.class
		).setParameter( "name", "identity" ).getSingleResult().getId() ).isNotEqualTo( 42L ) );
		// before execution generation
		scope.inTransaction( session -> session.persist( new AssignedEntity( 42L, "assigned" ) ) );
		scope.inSession( session -> assertThat( session.createQuery(
				"from AssignedEntity where name = :name",
				AssignedEntity.class
		).setParameter( "name", "assigned" ).getSingleResult().getId() ).isEqualTo( 42L ) );
	}

	@Test
	@SkipForDialect( dialectClass = SQLServerDialect.class, reason = "SQLServer does not support setting explicit values for identity columns" )
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "Oracle does not support setting explicit values for identity columns" )
	@SkipForDialect( dialectClass = SybaseASEDialect.class, reason = "Sybase does not support setting explicit values for identity columns" )
	public void testIdentityOrAssignedIdStateless(SessionFactoryScope scope) {
		// on execution generation
		scope.inStatelessTransaction( session -> session.insert( new AssignedEntity( "stateless_identity" ) ) );
		scope.inStatelessSession( session -> assertThat( session.createQuery(
				"from AssignedEntity where name = :name",
				AssignedEntity.class
		).setParameter( "name", "stateless_identity" ).getSingleResult().getId() ).isNotEqualTo( 23L ) );
		// before execution generation
		scope.inStatelessTransaction( session -> session.insert( new AssignedEntity( 23L, "stateless_assigned" ) ) );
		scope.inStatelessSession( session -> assertThat( session.createQuery(
				"from AssignedEntity where name = :name",
				AssignedEntity.class
		).setParameter( "name", "stateless_assigned" ).getSingleResult().getId() ).isEqualTo( 23L ) );
	}

	@Test
	@SkipForDialect( dialectClass = SQLServerDialect.class, reason = "SQLServer does not support setting explicit values for identity columns" )
	@SkipForDialect( dialectClass = OracleDialect.class, reason = "Oracle does not support setting explicit values for identity columns" )
	@SkipForDialect( dialectClass = SybaseASEDialect.class, reason = "Sybase does not support setting explicit values for identity columns" )
	public void testIdentityOrRandomId(SessionFactoryScope scope) {
		// on execution generation
		scope.inTransaction( session -> session.persist( new RandomEntity( "identity" ) ) );
		scope.inSession( session -> assertThat( session.createQuery(
				"from RandomEntity where name = :name",
				RandomEntity.class
		).setParameter( "name", "identity" ).getSingleResult().getId() ).isLessThan( 100L ) );
		// before execution generation
		scope.inTransaction( session -> session.persist( new RandomEntity( "random" ) ) );
		scope.inSession( session -> assertThat( session.createQuery(
				"from RandomEntity where name = :name",
				RandomEntity.class
		).setParameter( "name", "random" ).getSingleResult().getId() ).isGreaterThanOrEqualTo( 100L ) );
	}

	@Test
	public void testGeneratedPropInsert(SessionFactoryScope scope) {
		// on execution generation
		scope.inTransaction( session -> session.persist( new StringGeneratedEntity( 1L, "literal" ) ) );
		scope.inSession( session -> assertThat(
				session.find( StringGeneratedEntity.class, 1L ).getGeneratedProp()
		).startsWith( "literal" ) );
		// before execution generation
		scope.inTransaction( session -> session.persist( new StringGeneratedEntity( 2L, "generated" ) ) );
		scope.inSession( session -> assertThat(
				session.find( StringGeneratedEntity.class, 2L ).getGeneratedProp()
		).startsWith( "generated" ) );
	}

	@Test
	public void testGeneratedPropUpdate(SessionFactoryScope scope) {
		// on execution generation
		final int literalCount = scope.fromTransaction( session -> {
			final StringGeneratedEntity entity = new StringGeneratedEntity( 3L, "literal_inserted" );
			session.persist( entity );
			session.flush();
			assertThat( entity.getGeneratedProp() ).startsWith( "literal" );
			entity.setName( "literal_updated" );
			return Integer.parseInt( entity.getGeneratedProp().split( "_" )[1] );
		} );
		scope.inSession( session -> {
			final StringGeneratedEntity entity = session.find( StringGeneratedEntity.class, 3L );
			final String generatedProp = entity.getGeneratedProp();
			assertThat( generatedProp ).startsWith( "literal" );
			assertThat( Integer.parseInt( generatedProp.split( "_" )[1] ) ).isGreaterThan( literalCount );
		} );
		// before execution generation
		final int generatedCount = scope.fromTransaction( session -> {
			final StringGeneratedEntity entity = new StringGeneratedEntity( 4L, "generated_inserted" );
			session.persist( entity );
			session.flush();
			assertThat( entity.getGeneratedProp() ).startsWith( "generated" );
			entity.setName( "generated_updated" );
			return Integer.parseInt( entity.getGeneratedProp().split( "_" )[1] );
		} );
		scope.inSession( session -> {
			final StringGeneratedEntity entity = session.find( StringGeneratedEntity.class, 4L );
			final String generatedProp = entity.getGeneratedProp();
			assertThat( generatedProp ).startsWith( "generated" );
			assertThat( Integer.parseInt( generatedProp.split( "_" )[1] ) ).isGreaterThan( generatedCount );
		} );
	}

	@Entity( name = "AssignedEntity" )
	public static class AssignedEntity {
		@Id
		@GeneratedValue( generator = "identity_or_assigned" )
		@GenericGenerator( name = "identity_or_assigned", type = IdentityOrAssignedGenerator.class )
		private Long id;

		private String name;

		public AssignedEntity() {
		}

		public AssignedEntity(String name) {
			this.name = name;
		}

		public AssignedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "RandomEntity" )
	public static class RandomEntity {
		@Id
		@GeneratedValue( generator = "identity_or_random" )
		@GenericGenerator( name = "identity_or_random", type = IdentityOrRandomGenerator.class )
		private Long id;

		private String name;

		public RandomEntity() {
		}

		public RandomEntity(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@ValueGenerationType( generatedBy = LiteralOrGeneratedStringGenerator.class )
	@Retention( RUNTIME )
	@Target( { FIELD, METHOD } )
	public @interface GeneratedString {
		/**
		 * Specifies how the timestamp is generated. By default, it is generated
		 * in memory, which saves a round trip to the database.
		 */
		SourceType source() default SourceType.VM;
	}


	@Entity( name = "StringGeneratedEntity" )
	public static class StringGeneratedEntity {
		@Id
		private Long id;

		private String name;

		@GeneratedString
		private String generatedProp;

		public StringGeneratedEntity() {
		}

		public StringGeneratedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getGeneratedProp() {
			return generatedProp;
		}
	}

	public static class IdentityOrAssignedGenerator extends IdentityGenerator implements IdentifierGenerator {
		@Override
		public Object generate(SharedSessionContractImplementor session, Object object) {
			final EntityPersister entityPersister = session.getEntityPersister( null, object );
			return entityPersister.getIdentifier( object, session );
		}

		@Override
		public boolean generatedOnExecution() {
			return true;
		}

		@Override
		public boolean generatedOnExecution(Object owner, SharedSessionContractImplementor session) {
			return generate( session, owner, null, null ) == null;
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return INSERT_ONLY;
		}

		@Override
		public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) {
		}
	}

	public static class IdentityOrRandomGenerator extends IdentityGenerator implements BeforeExecutionGenerator {
		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return ThreadLocalRandom.current().nextLong( 100, 1_000 );
		}

		@Override
		public boolean generatedOnExecution() {
			return true;
		}

		@Override
		public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
			return !generatedBeforeExecution( entity, session );
		}

		@Override
		public boolean generatedBeforeExecution(Object entity, SharedSessionContractImplementor session) {
			return ( (RandomEntity) entity ).getName().contains( "random" );
		}
	}

	public static class LiteralOrGeneratedStringGenerator implements OnExecutionGenerator, BeforeExecutionGenerator {
		private int count;

		public LiteralOrGeneratedStringGenerator() {
			count = 0;
		}

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return "generated_" + count++;
		}

		@Override
		public boolean generatedOnExecution() {
			return true;
		}

		@Override
		public boolean generatedOnExecution(Object entity, SharedSessionContractImplementor session) {
			return !generatedBeforeExecution( entity, session );
		}

		@Override
		public boolean generatedBeforeExecution(Object entity, SharedSessionContractImplementor session) {
			return ( (StringGeneratedEntity) entity ).getName().contains( "generated" );
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.ALL;
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return true;
		}

		@Override
		public boolean writePropertyValue() {
			return false;
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return new String[] { "'literal_" + count++ + "'" };
		}
	}
}
