package org.hibernate.orm.test.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.java.LongJavaType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = BasicCollectionFlushTest.CollectionEntity.class,
		typeContributors = BasicCollectionFlushTest.CountingTypeContributor.class
)
@SessionFactory(generateStatistics = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStandardArrays.class)
@JiraKey("HHH-21149")
class BasicCollectionFlushTest {
	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void unchangedListIsComparedLinearlyOnFlush(SessionFactoryScope scope) {
		final var values = LongStream.range( 0, 1_000 ).boxed().toList();
		scope.inTransaction( session -> {
			final var entity = new CollectionEntity();
			entity.id = 1;
			entity.list = new ArrayList<>( values );
			session.persist( entity );
		} );
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		scope.inTransaction( session -> {
			session.find( CollectionEntity.class, 1 );
			final var elementType = (CountingLongJavaType) session.getTypeConfiguration()
					.getJavaTypeRegistry().resolveDescriptor( Long.class );
			elementType.comparisons = 0;

			session.flush();

			assertEquals( values.size(), elementType.comparisons );
		} );
		assertEquals( 0, statistics.getEntityUpdateCount() );
	}

	@Test
	void unchangedBagUsesTheIterationOrderFastPathOnFlush(SessionFactoryScope scope) {
		final var values = Collections.nCopies( 1_000, 1L );
		scope.inTransaction( session -> {
			final var entity = new CollectionEntity();
			entity.id = 3;
			entity.bag = new ArrayList<>( values );
			session.persist( entity );
		} );
		final var statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
			session.find( CollectionEntity.class, 3 );
			final var elementType = (CountingLongJavaType) session.getTypeConfiguration()
					.getJavaTypeRegistry().resolveDescriptor( Long.class );
			elementType.comparisons = 0;
			elementType.hashExtractions = 0;
			session.flush();
			assertEquals( values.size(), elementType.comparisons );
			assertEquals( 0, elementType.hashExtractions );
		} );
		assertEquals( 0, statistics.getEntityUpdateCount() );
	}

	@Test
	void bagMultiplicityChangeIsStoredOnFlush(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var entity = new CollectionEntity();
			entity.id = 2;
			entity.bag = new ArrayList<>( List.of( 1L, 1L, 2L ) );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			final var entity = session.find( CollectionEntity.class, 2 );
			entity.bag.clear();
			entity.bag.addAll( List.of( 1L, 2L, 2L ) );
			session.flush();
		} );

		scope.inTransaction( session -> assertThat( session.find( CollectionEntity.class, 2 ).bag )
				.containsExactlyInAnyOrder( 1L, 2L, 2L ) );
	}

	@Entity(name = "BasicCollectionEqualityEntity")
	@Table(name = "basic_collection_equality")
	static class CollectionEntity {
		@Id
		Integer id;

		@Column(name = "list_values")
		List<Long> list;

		@Column(name = "bag_values")
		Collection<Long> bag;
	}

	public static class CountingTypeContributor implements TypeContributor {
		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeJavaType( new CountingLongJavaType() );
		}
	}

	public static class CountingLongJavaType extends LongJavaType {
		private int comparisons;
		private int hashExtractions;

		@Override
		public int extractHashCode(Long value) {
			hashExtractions++;
			return super.extractHashCode( value );
		}

		@Override
		public boolean areEqual(Long one, Long another) {
			comparisons++;
			return super.areEqual( one, another );
		}
	}
}
