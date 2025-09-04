/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.delegate;

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.insert.AbstractReturningDelegate;
import org.hibernate.id.insert.AbstractSelectingDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.jboss.logging.Logger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link GeneratedValuesMutationDelegate efficient generated values retrieval}
 * when {@link GenerationType#IDENTITY identity} generated identifiers are involved.
 *
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MutationDelegateIdentityTest.IdentityOnly.class,
		MutationDelegateIdentityTest.IdentityAndValues.class,
		MutationDelegateIdentityTest.IdentityAndValuesAndRowId.class,
		MutationDelegateIdentityTest.IdentityAndValuesAndRowIdAndNaturalId.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
// Batch size is only enabled to make sure it's ignored when using mutation delegates
@ServiceRegistry( settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5" ) )
public class MutationDelegateIdentityTest {
	@Test
	public void testInsertGeneratedIdentityOnly(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate( scope, IdentityOnly.class, MutationType.INSERT );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityOnly entity = new IdentityOnly();
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isNull();

			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
			inspector.assertExecutedCount( delegate instanceof AbstractReturningDelegate ? 1 : 2 );
			assertThat( triggerable.wasTriggered() ).isFalse();
		} );
	}

	@Test
	public void testInsertGeneratedValuesAndIdentity(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				IdentityAndValues.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityAndValues entity = new IdentityAndValues();
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );

			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
			inspector.assertExecutedCount(
					delegate instanceof AbstractSelectingDelegate
							? 3
							: delegate != null && delegate.supportsArbitraryValues() ? 1 : 2
			);
		} );
	}

	@Test
	public void testUpdateGeneratedValuesAndIdentity(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				IdentityAndValues.class,
				MutationType.UPDATE
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final Long id = scope.fromTransaction( session -> {
			final IdentityAndValues entity = new IdentityAndValues();
			session.persist( entity );
			session.flush();
			return entity.getId();
		} );

		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityAndValues entity = session.find( IdentityAndValues.class, id );
			entity.setData( "changed" );
			session.flush();

			assertThat( entity.getUpdateDate() ).isNotNull();

			inspector.assertIsSelect( 0 );
			assertThat( inspector.getSqlQueries().get( 1 ) ).contains( "update " );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsArbitraryValues() ? 2 : 3
			);
		} );
	}

	@Test
	public void testInsertGeneratedValuesAndIdentityAndRowId(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				IdentityAndValuesAndRowId.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityAndValuesAndRowId entity = new IdentityAndValuesAndRowId();
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );

			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
			inspector.assertExecutedCount(
					delegate instanceof AbstractSelectingDelegate
							? 3
							: delegate != null && delegate.supportsArbitraryValues() ? 1 : 2
			);

			final boolean shouldHaveRowId = delegate != null && delegate.supportsRowId()
					&& scope.getSessionFactory().getJdbcServices().getDialect().rowId( "" ) != null;
			if ( shouldHaveRowId ) {
				// assert row-id was populated in entity entry
				final PersistenceContext pc = session.getPersistenceContextInternal();
				final EntityEntry entry = pc.getEntry( entity );
				assertThat( entry.getRowId() ).isNotNull();
			}

			// test update in same transaction
			inspector.clear();

			entity.setData( "changed" );
			session.flush();

			assertThat( entity.getUpdateDate() ).isNotNull();

			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "update " );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "id_column", shouldHaveRowId ? 0 : 1 );
		} );
		scope.inSession( session -> assertThat( session.find(
				IdentityAndValuesAndRowId.class,
				1
		).getUpdateDate() ).isNotNull() );
	}

	@Test
	public void testInsertGeneratedValuesAndIdentityAndRowIdAndNaturalId(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				IdentityAndValuesAndRowIdAndNaturalId.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final IdentityAndValuesAndRowIdAndNaturalId entity = new IdentityAndValuesAndRowIdAndNaturalId(
					"naturalid_1"
			);
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );

			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
			final boolean isUniqueKeyDelegate = delegate instanceof UniqueKeySelectingDelegate;
			inspector.assertExecutedCount(
					delegate == null || !delegate.supportsArbitraryValues() || isUniqueKeyDelegate ? 2 : 1
			);
			if ( isUniqueKeyDelegate ) {
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "data", 1 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "id_column", 1 );
			}

			final boolean shouldHaveRowId = delegate != null && delegate.supportsRowId()
					&& scope.getSessionFactory().getJdbcServices().getDialect().rowId( "" ) != null;
			if ( shouldHaveRowId ) {
				// assert row-id was populated in entity entry
				final PersistenceContext pc = session.getPersistenceContextInternal();
				final EntityEntry entry = pc.getEntry( entity );
				assertThat( entry.getRowId() ).isNotNull();
			}
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

	private Triggerable triggerable;

	@RegisterExtension
	public LoggerInspectionExtension logger = LoggerInspectionExtension.builder().setLogger(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, SqlExceptionHelper.class.getName() )
	).build();

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		triggerable = logger.watchForLogMessages( "SQL Error:" );
		triggerable.reset();
	}

	@Entity( name = "IdentityOnly" )
	public static class IdentityOnly {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "IdentityAndValues" )
	@SuppressWarnings( "unused" )
	public static class IdentityAndValues {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Date updateDate;

		private String data;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Date getUpdateDate() {
			return updateDate;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@RowId
	@Entity( name = "IdentityAndValuesAndRowId" )
	@SuppressWarnings( "unused" )
	public static class IdentityAndValuesAndRowId {
		@Id
		@Column( name = "id_column" )
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Date updateDate;

		private String data;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Date getUpdateDate() {
			return updateDate;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@RowId
	@Entity( name = "IdentityAndValuesAndRowIdAndNaturalId" )
	@SuppressWarnings( "unused" )
	public static class IdentityAndValuesAndRowIdAndNaturalId {
		@Id
		@Column( name = "id_column" )
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@NaturalId
		private String data;

		public IdentityAndValuesAndRowIdAndNaturalId() {
		}

		private IdentityAndValuesAndRowIdAndNaturalId(String data) {
			this.data = data;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
