package org.hibernate.orm.test.boot.models.xml.attr;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ManyToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A {@code <many-to-one/>} may have both a {@code <join-column/>} and a {@code <property-ref/>},
 * here joining the primary key of its owner to the referenced attribute.
 */
@JiraKey("HHH-21112")
@SuppressWarnings("JUnitMalformedDeclaration")
public class ManyToOnePropertyRefTests {
	@Test
	@DomainModel(xmlMappings = "mappings/models/attr/many-to-one/property-ref.xml")
	void testJoinColumnWithPropertyRef(DomainModelScope scope) {
		final var child = scope.getEntityBinding( Child.class );
		assertThat( child.getProperty( "parent" ).getValue() )
				.isInstanceOfSatisfying( ManyToOne.class,
						parent -> assertThat( parent.getReferencedPropertyName() ).isEqualTo( "code" ) );
		assertThat( child.getTable().getColumns() ).extracting( Column::getName ).containsExactly( "id" );
		assertThat( child.getTable().getForeignKeyCollection() ).singleElement().satisfies( foreignKey -> {
			assertThat( foreignKey.getColumns() ).extracting( Column::getName ).containsExactly( "id" );
			assertThat( foreignKey.getReferencedColumns() ).extracting( Column::getName ).containsExactly( "code" );
		} );
	}

	@SuppressWarnings("unused")
	public static class Parent {
		private Long id;
		private Long code;
	}

	@SuppressWarnings("unused")
	public static class Child {
		private Long id;
		private Parent parent;
	}
}
