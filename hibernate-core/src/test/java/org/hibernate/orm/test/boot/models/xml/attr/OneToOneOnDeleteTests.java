package org.hibernate.orm.test.boot.models.xml.attr;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.mapping.Column;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code <on-delete/>} in a {@code <one-to-one/>} applies to its foreign key.
 */
@JiraKey("HHH-21111")
@SuppressWarnings("JUnitMalformedDeclaration")
public class OneToOneOnDeleteTests {
	@Test
	@DomainModel(xmlMappings = "mappings/models/attr/one-to-one/on-delete.xml")
	void testOnDelete(DomainModelScope scope) {
		final var table = scope.getEntityBinding( Child.class ).getTable();
		assertThat( table.getForeignKeyCollection() ).hasSize( 1 );
		final var foreignKey = table.getForeignKeyCollection().iterator().next();
		assertThat( foreignKey.getColumns() ).extracting( Column::getName ).containsExactly( "id" );
		assertThat( foreignKey.getOnDeleteAction() ).isEqualTo( OnDeleteAction.CASCADE );
	}

	@SuppressWarnings("unused")
	public static class Parent {
		private Long id;
	}

	@SuppressWarnings("unused")
	public static class Child {
		private Long id;
		private Parent parent;
	}
}
