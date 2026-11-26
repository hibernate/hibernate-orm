package org.hibernate.orm.test.mapping.identifier;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.PartitionKey;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Where the partition key belongs to the primary key, a partition key column
 * of a secondary table belongs to the primary key of the secondary table.
 */
@DomainModel(annotatedClasses = SecondaryTablePartitionKeyTest.Parcel.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = JdbcSettings.DIALECT,
		value = "org.hibernate.orm.test.mapping.identifier.SecondaryTablePartitionKeyTest$PartitionKeyInPrimaryKeyDialect"))
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-21280")
public class SecondaryTablePartitionKeyTest {
	@Test
	void primaryKeys(DomainModelScope scope) {
		final var entityBinding = scope.getEntityBinding( Parcel.class );
		assertThat( entityBinding.getTable().getPrimaryKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "id" );
		assertThat( entityBinding.getJoins().get( 0 ).getTable().getPrimaryKey().getColumns() )
				.extracting( org.hibernate.mapping.Column::getName )
				.containsExactly( "id", "region" );
	}

	@Test
	void persistAndRemove(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Parcel( 1, "eu" ) ) );
		scope.inTransaction( session -> {
			final var parcel = session.find( Parcel.class, 1 );
			assertThat( parcel.region ).isEqualTo( "eu" );
			session.remove( parcel );
		} );
		scope.inTransaction( session -> assertThat( session.find( Parcel.class, 1 ) ).isNull() );
	}

	@Entity(name = "Parcel")
	@Table(name = "partition_parcel")
	@SecondaryTable(name = "partition_parcel_detail")
	public static class Parcel {
		@Id
		Integer id;
		@PartitionKey
		@Column(table = "partition_parcel_detail", nullable = false)
		String region;

		Parcel() {
		}

		Parcel(Integer id, String region) {
			this.id = id;
			this.region = region;
		}
	}

	public static class PartitionKeyInPrimaryKeyDialect extends H2Dialect {
		@Override
		public boolean addPartitionKeyToPrimaryKey() {
			return true;
		}
	}
}
