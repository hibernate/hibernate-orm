package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Temporal;
import org.hibernate.boot.MetadataSources;
import org.hibernate.mapping.Column;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;

@ServiceRegistry(settings = @Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"))
class TemporalVersionExclusionTest {
	@Test
	void requiresTemporalExclusionOnTheSameAttribute(ServiceRegistryScope scope) {
		var sources = new MetadataSources( scope.getRegistry() ).addAnnotatedClass( VersionedHistory.class );
		assertThatThrownBy( sources::buildMetadata )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( "VersionedHistory.counter" )
				.hasMessageContaining( "@ExcludedFromVersioning" )
				.hasMessageContaining( "@Temporal.Excluded" )
				.hasMessageContaining( "HISTORY_TABLE" );
	}

	@Test
	@DomainModel(annotatedClasses = PairedExclusions.class)
	@SessionFactory
	void pairedExclusionsKeepOneRevision(SessionFactoryScope scope) {
		var entity = scope.getMetadataImplementor().getEntityBinding( PairedExclusions.class.getName() );
		assertThat( entity.getAuxiliaryTable().getPrimaryKey().getColumns() )
				.extracting( Column::getName ).containsExactly( "id", "version" );

		scope.inTransaction( session -> session.persist( new PairedExclusions() ) );
		scope.inTransaction( session -> session.find( PairedExclusions.class, 1 ).counter++ );
		scope.inSession( session -> {
			var revision = session.createNativeQuery(
					"select version, counter from excluded_counter_history", Object[].class ).getSingleResult();
			assertThat( revision ).extracting( value -> ( (Number) value ).longValue() ).containsExactly( 0L, 1L );
		} );
	}

	@Test
	void allowsVersionExclusionOnUnversionedHistoryAndNonTemporalEntities(ServiceRegistryScope scope) {
		new MetadataSources( scope.getRegistry() )
				.addAnnotatedClass( UnversionedHistory.class )
				.addAnnotatedClass( NonTemporal.class )
				.buildMetadata().buildSessionFactory().close();
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE"))
	void allowsVersionExclusionWithSingleTable(ServiceRegistryScope scope) {
		new MetadataSources( scope.getRegistry() ).addAnnotatedClass( VersionedHistory.class )
				.buildMetadata().buildSessionFactory().close();
	}

	@MappedSuperclass
	static class WithCounter {
		@Id int id;
		@ExcludedFromVersioning int counter;
	}

	@Entity
	@Temporal
	static class VersionedHistory extends WithCounter {
		@Version int version;
		@Temporal.Excluded String note;
	}

	@Entity
	@Temporal
	static class UnversionedHistory extends WithCounter {}

	@Entity
	static class NonTemporal extends WithCounter {
		@Version int version;
	}

	@Entity
	@Temporal
	@Table(name = "excluded_counter")
	static class PairedExclusions {
		@Id int id = 1;
		@Version int version;
		@ExcludedFromVersioning @Temporal.Excluded int counter;
	}
}
