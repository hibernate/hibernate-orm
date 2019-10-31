/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.AttributeConverter;
import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.DomainMetamodel;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				PluralAttributeTests.SimpleEntity.class,
				PluralAttributeTests.EntityContainingLists.class,
				PluralAttributeTests.EntityContainingSets.class,
				PluralAttributeTests.Component.class
		}
)
@ServiceRegistry
@SessionFactory
@SuppressWarnings("WeakerAccess")
public class PluralAttributeTests {

	@Test
	public void testLists(SessionFactoryScope scope) {
		final DomainMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityMappingType containerEntityDescriptor = domainModel.getEntityDescriptor( EntityContainingLists.class );

		assertThat( containerEntityDescriptor.getNumberOfAttributeMappings(), is( 6 ) );

		final AttributeMapping listOfBasics = containerEntityDescriptor.findAttributeMapping( "listOfBasics" );
		assertThat( listOfBasics, notNullValue() );

		final AttributeMapping listOfConvertedBasics = containerEntityDescriptor.findAttributeMapping( "listOfConvertedBasics" );
		assertThat( listOfConvertedBasics, notNullValue() );


		final AttributeMapping listOfEnums = containerEntityDescriptor.findAttributeMapping( "listOfEnums" );
		assertThat( listOfEnums, notNullValue() );

		final AttributeMapping listOfComponents = containerEntityDescriptor.findAttributeMapping( "listOfComponents" );
		assertThat( listOfComponents, notNullValue() );

		final AttributeMapping listOfEntities = containerEntityDescriptor.findAttributeMapping( "listOfEntities" );
		assertThat( listOfEntities, notNullValue() );
	}

	@Test
	public void testSets(SessionFactoryScope scope) {
		final DomainMetamodel domainModel = scope.getSessionFactory().getDomainModel();
		final EntityMappingType containerEntityDescriptor = domainModel.getEntityDescriptor( EntityContainingSets.class );

		assertThat( containerEntityDescriptor.getNumberOfAttributeMappings(), is( 6 ) );

		final AttributeMapping setOfBasics = containerEntityDescriptor.findAttributeMapping( "setOfBasics" );
		assertThat( setOfBasics, notNullValue() );

		final AttributeMapping setOfConvertedBasics = containerEntityDescriptor.findAttributeMapping( "setOfConvertedBasics" );
		assertThat( setOfConvertedBasics, notNullValue() );


		final AttributeMapping setOfEnums = containerEntityDescriptor.findAttributeMapping( "setOfEnums" );
		assertThat( setOfEnums, notNullValue() );

		final AttributeMapping setOfComponents = containerEntityDescriptor.findAttributeMapping( "setOfComponents" );
		assertThat( setOfComponents, notNullValue() );

		final AttributeMapping setOfEntities = containerEntityDescriptor.findAttributeMapping( "setOfEntities" );
		assertThat( setOfEntities, notNullValue() );
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
		@Convert( converter = Converter.class )
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

	@Entity( name = "EntityContainingSets" )
	@Table( name = "entity_containing_sets" )
	public static class EntityContainingSets {
		private Integer id;
		private String name;

		private Set<String> setOfBasics;
		private Set<EnumValue> setOfConvertedBasics;
		private Set<EnumValue> setOfEnums;
		private Set<Component> setOfComponents;
		private Set<SimpleEntity> setOfEntities;

		public EntityContainingSets() {
		}

		public EntityContainingSets(Integer id, String name) {
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
		public Set<String> getSetOfBasics() {
			return setOfBasics;
		}

		public void setSetOfBasics(Set<String> setOfBasics) {
			this.setOfBasics = setOfBasics;
		}

		public void addBasic(String value) {
			if ( setOfBasics == null ) {
				setOfBasics = new HashSet<>();
			}
			setOfBasics.add( value );
		}

		@ElementCollection
		@Convert( converter = Converter.class )
		public Set<EnumValue> getSetOfConvertedBasics() {
			return setOfConvertedBasics;
		}

		public void setSetOfConvertedBasics(Set<EnumValue> setOfConvertedBasics) {
			this.setOfConvertedBasics = setOfConvertedBasics;
		}

		public void addConvertedBasic(EnumValue value) {
			if ( setOfConvertedBasics == null ) {
				setOfConvertedBasics = new HashSet<>();
			}
			setOfConvertedBasics.add( value );
		}

		@ElementCollection
		@Enumerated( EnumType.STRING )
		public Set<EnumValue> getSetOfEnums() {
			return setOfEnums;
		}

		public void setSetOfEnums(Set<EnumValue> setOfEnums) {
			this.setOfEnums = setOfEnums;
		}

		public void addEnum(EnumValue value) {
			if ( setOfEnums == null ) {
				setOfEnums = new HashSet<>();
			}
			setOfEnums.add( value );
		}

		@ElementCollection
		@Embedded
		public Set<Component> getSetOfComponents() {
			return setOfComponents;
		}

		public void setSetOfComponents(Set<Component> setOfComponents) {
			this.setOfComponents = setOfComponents;
		}

		public void addComponent(Component value) {
			if ( setOfComponents == null ) {
				setOfComponents = new HashSet<>();
			}
			setOfComponents.add( value );
		}

		@OneToMany( cascade = CascadeType.ALL )
		public Set<SimpleEntity> getSetOfEntities() {
			return setOfEntities;
		}

		public void setSetOfEntities(Set<SimpleEntity> setOfEntities) {
			this.setOfEntities = setOfEntities;
		}

		public void addSimpleEntity(SimpleEntity value) {
			if ( setOfEntities == null ) {
				setOfEntities = new HashSet<>();
			}
			setOfEntities.add( value );
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
