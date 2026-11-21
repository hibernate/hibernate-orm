package org.hibernate.orm.test.softdelete.collections;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.type.YesNoConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		SoftDeleteElementCollectionConverterTest.ActiveOwner.class,
		SoftDeleteElementCollectionConverterTest.DeletedOwner.class
} )
@SessionFactory
@ServiceRegistry( settings = {
		@Setting( name = "hibernate.hbm2ddl.halt_on_error", value = "true" ),
		@Setting( name = "jakarta.persistence.schema-generation.database.action", value = "create" )
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-20585" )
class SoftDeleteElementCollectionConverterTest {
	@AfterAll
	void dropSchema(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().dropMappedObjects( false );
	}

	@Test
	void activeConverterUsesTrueForLiveRows(DomainModelScope scope) {
		assertLiveRowLiteral( scope, ActiveOwner.class, 'Y' );
	}

	@Test
	void deletedConverterUsesFalseForLiveRows(DomainModelScope scope) {
		assertLiveRowLiteral( scope, DeletedOwner.class, 'N' );
	}

	private static void assertLiveRowLiteral(DomainModelScope scope, Class<?> ownerType, char liveRowValue) {
		final var collection = scope.getDomainModel().getCollectionBinding( ownerType.getName() + ".elements" );
		final var column = collection.getSoftDeleteColumn();
		final var resolution = ( (BasicValue) column.getValue() ).resolve();
		@SuppressWarnings("unchecked")
		final var converter = (BasicValueConverter<Boolean, ?>) resolution.getValueConverter();
		// The resolved ACTIVE converter reverses its input before invoking YesNoConverter.
		assertThat( converter.toRelationalValue( false ) ).isEqualTo( liveRowValue );

		final var table = collection.getCollectionTable();
		assertThat( table.hasPrimaryKey() ).isFalse();
		assertThat( table.getUniqueKeys() ).isEmpty();
		assertThat( table.getIndexes() ).isEmpty();
	}

	@Test
	void activeConvertedCollectionSupportsRepeatedDeleteCycles(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new ActiveOwner();
			owner.id = 1L;
			session.persist( owner );
		} );
		for ( int cycle = 0; cycle < 3; cycle++ ) {
			scope.inTransaction( session -> session.get( ActiveOwner.class, 1L ).elements.add( "value" ) );
			scope.inTransaction( session -> {
				final var owner = session.get( ActiveOwner.class, 1L );
				assertThat( owner.elements ).containsExactly( "value" );
				owner.elements.remove( "value" );
			} );
		}
		scope.inTransaction( session -> assertThat( session.get( ActiveOwner.class, 1L ).elements ).isEmpty() );
	}

	@Test
	void deletedConvertedCollectionSupportsRepeatedDeleteCycles(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var owner = new DeletedOwner();
			owner.id = 1L;
			session.persist( owner );
		} );
		for ( int cycle = 0; cycle < 3; cycle++ ) {
			scope.inTransaction( session -> session.get( DeletedOwner.class, 1L ).elements.add( "value" ) );
			scope.inTransaction( session -> {
				final var owner = session.get( DeletedOwner.class, 1L );
				assertThat( owner.elements ).containsExactly( "value" );
				owner.elements.remove( "value" );
			} );
		}
		scope.inTransaction( session -> assertThat( session.get( DeletedOwner.class, 1L ).elements ).isEmpty() );
	}

	@Entity( name = "ActiveConvertedCollectionOwner" )
	static class ActiveOwner {
		@Id
		Long id;

		@ElementCollection
		@CollectionTable( name = "active_converted_elements" )
		@SoftDelete( strategy = SoftDeleteType.ACTIVE, converter = YesNoConverter.class )
		Set<String> elements = new LinkedHashSet<>();
	}

	@Entity( name = "DeletedConvertedCollectionOwner" )
	static class DeletedOwner {
		@Id
		Long id;

		@ElementCollection
		@CollectionTable( name = "deleted_converted_elements" )
		@SoftDelete( strategy = SoftDeleteType.DELETED, converter = YesNoConverter.class )
		Set<String> elements = new LinkedHashSet<>();
	}
}
