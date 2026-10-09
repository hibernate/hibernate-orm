/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.softdelete.collections;

import java.util.Set;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.type.YesNoConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;

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
@Jira( "https://hibernate.atlassian.net/browse/HHH-20585" )
class SoftDeleteElementCollectionConverterTest {
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

		final var dialect = scope.getDomainModel().getDatabase().getDialect();
		assertThat( collection.getCollectionTable().getIndexes().values() )
				.singleElement()
				.satisfies( index -> {
					assertThat( index.isUnique() ).isTrue();
					assertThat( index.getSelectables() ).hasSize( 3 );
					assertThat( index.getSelectables().get( 2 ).getText() ).isEqualTo(
							"(case when " + column.getQuotedName( dialect ) + " = '" + liveRowValue + "' then 1 end)"
					);
				} );
	}

	@Entity( name = "ActiveConvertedCollectionOwner" )
	static class ActiveOwner {
		@Id
		Long id;

		@ElementCollection
		@CollectionTable( name = "active_converted_elements" )
		@SoftDelete( strategy = SoftDeleteType.ACTIVE, converter = YesNoConverter.class )
		Set<String> elements;
	}

	@Entity( name = "DeletedConvertedCollectionOwner" )
	static class DeletedOwner {
		@Id
		Long id;

		@ElementCollection
		@CollectionTable( name = "deleted_converted_elements" )
		@SoftDelete( strategy = SoftDeleteType.DELETED, converter = YesNoConverter.class )
		Set<String> elements;
	}
}
