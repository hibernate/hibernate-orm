package org.hibernate.orm.test.id.enhanced;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TableStructure#getNextValues spaces returned source values by incrementSize even when the
 * optimizer does not apply the increment size to source values (hilo, legacy-hilo), while the
 * table row only advances by the batch size, so later batches hand out overlapping id ranges.
 * The optimizer is selected through {@link MappingSettings#PREFERRED_POOLED_OPTIMIZER}.
 */
class TableStructureBatchSpacingTest {

	@Test
	@DomainModel(annotatedClasses = BatchEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = MappingSettings.PREFERRED_POOLED_OPTIMIZER, value = "hilo"))
	void hiLo(SessionFactoryScope scope) {
		batchesDoNotReturnOverlappingIds( scope );
	}

	@Test
	@DomainModel(annotatedClasses = BatchEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = MappingSettings.PREFERRED_POOLED_OPTIMIZER, value = "legacy-hilo"))
	void legacyHiLo(SessionFactoryScope scope) {
		batchesDoNotReturnOverlappingIds( scope );
	}

	/** Control: applies the increment size to source values, so the table row advances accordingly. */
	@Test
	@DomainModel(annotatedClasses = BatchEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(name = MappingSettings.PREFERRED_POOLED_OPTIMIZER, value = "pooled"))
	void pooled(SessionFactoryScope scope) {
		batchesDoNotReturnOverlappingIds( scope );
	}

	private void batchesDoNotReturnOverlappingIds(SessionFactoryScope scope) {
		final Set<Long> ids = new HashSet<>();
		final List<Long> duplicates = new ArrayList<>();
		// 153 ids = 3 buckets of 51 per batch: the table row advances by 3 per batch, but the returned
		// source values are v, v+51, v+102, so source value v+51 is handed out again 17 batches later
		for ( int round = 0; round < 25; round++ ) {
			final List<BatchEntity> batch = newEntities( 153 );
			scope.inStatelessTransaction( session -> session.insertMultiple( batch ) );
			for ( BatchEntity e : batch ) {
				if ( !ids.add( e.id ) ) {
					duplicates.add( e.id );
				}
			}
		}
		assertThat( duplicates ).as( "duplicate ids generated" ).isEmpty();
	}

	private static List<BatchEntity> newEntities(int n) {
		final List<BatchEntity> list = new ArrayList<>();
		for ( int i = 0; i < n; i++ ) {
			list.add( new BatchEntity() );
		}
		return list;
	}

	@Entity(name = "BatchEntity")
	static class BatchEntity {
		@Id
		@GenericGenerator(type = SequenceStyleGenerator.class, parameters = {
				@Parameter(name = SequenceStyleGenerator.FORCE_TBL_PARAM, value = "true"),
				@Parameter(name = "increment_size", value = "51")
		})
		Long id;
		String name;
	}
}
