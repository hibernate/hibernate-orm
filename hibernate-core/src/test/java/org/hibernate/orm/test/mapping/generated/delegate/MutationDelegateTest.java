/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.delegate;

import java.util.Date;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.insert.UniqueKeySelectingDelegate;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generic tests regarding {@link GeneratedValuesMutationDelegate efficient generated values retrieval}.
 *
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MutationDelegateTest.ValuesOnly.class,
		MutationDelegateTest.ValuesAndRowId.class,
		MutationDelegateTest.ValuesAndNaturalId.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
// Batch size is only enabled to make sure it's ignored when using mutation delegates
@ServiceRegistry( settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "5" ) )
public class MutationDelegateTest {
	@Test
	public void testInsertGeneratedValues(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate( scope, ValuesOnly.class, MutationType.INSERT );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final ValuesOnly entity = new ValuesOnly( 1 );
			session.persist( entity );
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "default_name" );

			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsArbitraryValues() ? 1 : 2
			);
		} );
	}

	@Test
	public void testUpdateGeneratedValues(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate( scope, ValuesOnly.class, MutationType.UPDATE );
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final ValuesOnly entity = new ValuesOnly( 2 );
			session.persist( entity );
		} );

		inspector.clear();

		scope.inTransaction( session -> {
			final ValuesOnly entity = session.find( ValuesOnly.class, 2 );
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
	public void testGeneratedValuesAndRowId(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				ValuesAndRowId.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final ValuesAndRowId entity = new ValuesAndRowId( 1 );
			session.persist( entity );
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "default_name" );

			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsArbitraryValues() ? 1 : 2
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
		scope.inSession( session -> assertThat( session.find( ValuesAndRowId.class, 1 ).getUpdateDate() ).isNotNull() );
	}

	@Test
	public void testInsertGeneratedValuesAndNaturalId(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				ValuesAndNaturalId.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final ValuesAndNaturalId entity = new ValuesAndNaturalId( 1, "natural_1" );
			session.persist( entity );
			session.flush();

			assertThat( entity.getName() ).isEqualTo( "default_name" );

			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
			final boolean isUniqueKeyDelegate = delegate instanceof UniqueKeySelectingDelegate;
			inspector.assertExecutedCount(
					delegate == null || isUniqueKeyDelegate ? 2 : 1
			);
			if ( isUniqueKeyDelegate ) {
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "data", 1 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "id_column", 0 );
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

	@Entity( name = "ValuesOnly" )
	@SuppressWarnings( "unused" )
	public static class ValuesOnly {
		@Id
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Date updateDate;

		@SuppressWarnings( "FieldCanBeLocal" )
		private String data;

		public ValuesOnly() {
		}

		private ValuesOnly(Integer id) {
			this.id = id;
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
	@Entity( name = "ValuesAndRowId" )
	@SuppressWarnings( "unused" )
	public static class ValuesAndRowId {
		@Id
		@Column( name = "id_column" )
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Date updateDate;

		@SuppressWarnings( "FieldCanBeLocal" )
		private String data;

		public ValuesAndRowId() {
		}

		private ValuesAndRowId(Integer id) {
			this.id = id;
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

	@Entity( name = "ValuesAndNaturalId" )
	@SuppressWarnings( "unused" )
	public static class ValuesAndNaturalId {
		@Id
		@Column( name = "id_column" )
		private Integer id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@NaturalId
		private String data;

		public ValuesAndNaturalId() {
		}

		private ValuesAndNaturalId(Integer id, String data) {
			this.id = id;
			this.data = data;
		}

		public String getName() {
			return name;
		}
	}
}
