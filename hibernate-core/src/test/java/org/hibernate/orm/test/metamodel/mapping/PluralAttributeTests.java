/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				PluralAttributeTests.SimpleEntity.class,
				PluralAttributeTests.EntityContainingLists.class,
				PluralAttributeTests.Component.class
		}
)
@ServiceRegistry
@SessionFactory
@SuppressWarnings("WeakerAccess")
public class PluralAttributeTests {

	@Test
	public void testLists(SessionFactoryScope scope) {
		System.out.println( "test" );
	}

	@BeforeAll
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityContainingLists entityContainingLists = new EntityContainingLists( 1, "first" );

					entityContainingLists.addBasic( "abc" );
					entityContainingLists.addBasic( "def" );
					entityContainingLists.addBasic( "ghi" );

					entityContainingLists.addConvertedBasic( EnumValue.TWO );

					entityContainingLists.addEnum( EnumValue.ONE );
					entityContainingLists.addEnum( EnumValue.THREE );

					entityContainingLists.addComponent( new Component( "first-a1", "first-another-a1" ) );
					entityContainingLists.addComponent( new Component( "first-a2", "first-another-a2" ) );

					entityContainingLists.addSimpleEntity( new SimpleEntity( 1, "simple-1" ) );
					entityContainingLists.addSimpleEntity( new SimpleEntity( 2, "simple-2" ) );

					session.save( entityContainingLists );
				}
		);
	}

	@AfterAll
	public void deleteTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.doWork(
						conn -> {
							try ( Statement stmnt = conn.createStatement() ) {
								stmnt.execute( "delete from EntityContainingLists_listOfEnums" );
								stmnt.execute( "delete from EntityContainingLists_listOfConvertedBasics" );
								stmnt.execute( "delete from EntityContainingLists_listOfComponents" );
								stmnt.execute( "delete from EntityContainingLists_listOfBasics" );
								stmnt.execute( "delete from entity_containing_lists_simple_entity" );
								stmnt.execute( "delete from entity_containing_lists" );
							}
						}
				)
		);
	}

	public enum EnumValue {
		ONE( "first" ),
		TWO( "second" ),
		THREE( "third" );

		private final String code;

		EnumValue(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}

		public static EnumValue fromCode(String code) {
			if ( code == null || code.isEmpty() ) {
				return null;
			}

			switch ( code ) {
				case "first" : {
					return ONE;
				}
				case "second" : {
					return TWO;
				}
				case "third" : {
					return THREE;
				}
				default: {
					throw new RuntimeException( "Could not convert enum code : " + code );
				}
			}
		}
	}

	public static class Converter implements AttributeConverter<EnumValue,String> {
		@Override
		public String convertToDatabaseColumn(EnumValue domainValue) {
			return domainValue == null ? null : domainValue.getCode();
		}

		@Override
		public EnumValue convertToEntityAttribute(String dbData) {
			return EnumValue.fromCode( dbData );
		}
	}

	@Entity( name = "EntityContainingLists" )
	@Table( name = "entity_containing_lists" )
	public static class EntityContainingLists {
		private Integer id;
		private String name;

		private List<String> listOfBasics;
		private List<EnumValue> listOfConvertedBasics;
		private List<EnumValue> listOfEnums;
		private List<Component> listOfComponents;
		private List<SimpleEntity> listOfEntities;

		public EntityContainingLists() {
		}

		public EntityContainingLists(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ElementCollection
		@OrderColumn
		public List<String> getListOfBasics() {
			return listOfBasics;
		}

		public void setListOfBasics(List<String> listOfBasics) {
			this.listOfBasics = listOfBasics;
		}

		public void addBasic(String basic) {
			if ( listOfBasics == null ) {
				listOfBasics = new ArrayList<>();
			}
			listOfBasics.add( basic );
		}

		@ElementCollection
		@OrderColumn
		public List<EnumValue> getListOfConvertedBasics() {
			return listOfConvertedBasics;
		}

		public void setListOfConvertedBasics(List<EnumValue> listOfConvertedBasics) {
			this.listOfConvertedBasics = listOfConvertedBasics;
		}

		public void addConvertedBasic(EnumValue value) {
			if ( listOfConvertedBasics == null ) {
				listOfConvertedBasics = new ArrayList<>();
			}
			listOfConvertedBasics.add( value );
		}

		@ElementCollection
		@Enumerated( EnumType.STRING )
		@OrderColumn
		public List<EnumValue> getListOfEnums() {
			return listOfEnums;
		}

		public void setListOfEnums(List<EnumValue> listOfEnums) {
			this.listOfEnums = listOfEnums;
		}

		public void addEnum(EnumValue value) {
			if ( listOfEnums == null ) {
				listOfEnums = new ArrayList<>();
			}
			listOfEnums.add( value );
		}

		@ElementCollection
		@OrderColumn
		public List<Component> getListOfComponents() {
			return listOfComponents;
		}

		public void setListOfComponents(List<Component> listOfComponents) {
			this.listOfComponents = listOfComponents;
		}

		public void addComponent(Component value) {
			if ( listOfComponents == null ) {
				listOfComponents = new ArrayList<>();
			}
			listOfComponents.add( value );
		}

		@OneToMany( cascade = CascadeType.ALL )
		@OrderColumn
		public List<SimpleEntity> getListOfEntities() {
			return listOfEntities;
		}

		public void setListOfEntities(List<SimpleEntity> listOfEntities) {
			this.listOfEntities = listOfEntities;
		}

		public void addSimpleEntity(SimpleEntity value) {
			if ( listOfEntities == null ) {
				listOfEntities = new ArrayList<>();
			}
			listOfEntities.add( value );
		}
	}

	@Entity( name = "SimpleEntity" )
	@Table( name = "simple_entity" )
	public static class SimpleEntity {
		private Integer id;
		private String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class Component {
		private String anAttribute;
		private String anotherAttribute;

		public Component() {
		}

		public Component(String anAttribute, String anotherAttribute) {
			this.anAttribute = anAttribute;
			this.anotherAttribute = anotherAttribute;
		}

		public String getAnAttribute() {
			return anAttribute;
		}

		public void setAnAttribute(String anAttribute) {
			this.anAttribute = anAttribute;
		}

		public String getAnotherAttribute() {
			return anotherAttribute;
		}

		public void setAnotherAttribute(String anotherAttribute) {
			this.anotherAttribute = anotherAttribute;
		}
	}
}
