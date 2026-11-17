package org.hibernate.orm.test.id.enhanced;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.TableGenerator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@link TableGenerator} with a hilo optimizer does not apply the increment size to the values
 * stored in its table, so the row only advances by one per bucket. Batches that span several
 * buckets must space their returned source values accordingly, otherwise later batches hand out
 * overlapping id ranges.
 */
class TableGeneratorBatchSpacingTest {

	@Test
	@DomainModel(annotatedClasses = HiLoEntity.class)
	@SessionFactory
	void hiLo(SessionFactoryScope scope) {
		final Set<Long> ids = new HashSet<>();
		final List<Long> duplicates = new ArrayList<>();
		// 153 ids = 3 buckets of 51 per batch
		for ( int round = 0; round < 25; round++ ) {
			final List<HiLoEntity> batch = new ArrayList<>();
			for ( int i = 0; i < 153; i++ ) {
				batch.add( new HiLoEntity() );
			}
			scope.inStatelessTransaction( session -> session.insertMultiple( batch ) );
			for ( HiLoEntity e : batch ) {
				if ( !ids.add( e.id ) ) {
					duplicates.add( e.id );
				}
			}
		}
		assertThat( duplicates ).as( "duplicate ids generated" ).isEmpty();
	}

	@Entity(name = "HiLoEntity")
	static class HiLoEntity {
		@Id
		@GenericGenerator(type = TableGenerator.class, parameters = {
				@Parameter(name = "optimizer", value = "hilo"),
				@Parameter(name = "increment_size", value = "51")
		})
		Long id;
		String name;
	}
}
