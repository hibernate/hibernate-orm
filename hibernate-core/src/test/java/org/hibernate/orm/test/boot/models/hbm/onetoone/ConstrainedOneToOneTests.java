package org.hibernate.orm.test.boot.models.hbm.onetoone;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.OneToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A constrained {@code <one-to-one/>} maps a foreign key on the primary key
 * of its owner, whether or not the {@code hbm.xml} mapping is transformed.
 */
@JiraKey("HHH-21112")
@SuppressWarnings("JUnitMalformedDeclaration")
public class ConstrainedOneToOneTests {
	private static final String MAPPING = "mappings/models/hbm/onetoone/constrained.hbm.xml";

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "false"))
	@DomainModel(xmlMappings = MAPPING)
	void testHbm(DomainModelScope scope) {
		verify( scope );
	}

	@Test
	@ServiceRegistry(settings = @Setting(name = MappingSettings.TRANSFORM_HBM_XML, value = "true"))
	@DomainModel(xmlMappings = MAPPING)
	void testTransformed(DomainModelScope scope) {
		verify( scope );
	}

	private static void verify(DomainModelScope scope) {
		final var child = scope.getEntityBinding( Child.class );
		final var parent = child.getProperty( "parent" );
		assertThat( parent.getValue() ).isInstanceOf( OneToOne.class );
		assertThat( ( (OneToOne) parent.getValue() ).isConstrained() ).isTrue();

		final var table = child.getTable();
		assertThat( table.getColumns() ).extracting( Column::getName ).containsExactly( "id" );
		assertThat( table.getForeignKeyCollection() ).hasSize( 1 );
		final var foreignKey = table.getForeignKeyCollection().iterator().next();
		assertThat( foreignKey.getColumns() ).extracting( Column::getName ).containsExactly( "id" );
		assertThat( foreignKey.getReferencedEntityName() ).isEqualTo( Parent.class.getName() );
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
