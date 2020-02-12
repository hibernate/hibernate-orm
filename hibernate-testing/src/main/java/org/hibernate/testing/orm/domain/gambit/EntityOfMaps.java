/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.gambit;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;

import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;

/**
 * @author Steve Ebersole
 * @author Fabio Massimo Ercoli
 */
@SuppressWarnings("unused")
@Entity
public class EntityOfMaps {
	private Integer id;
	private String name;

	private Map<String, String> basicByBasic;

	private SortedMap<String, String> sortedBasicByBasic;
	private SortedMap<String, String> sortedBasicByBasicWithComparator;
	private SortedMap<String, String> sortedBasicByBasicWithSortNaturalByDefault;

	private Map<EnumValue, String> basicByEnum;
	private Map<EnumValue, String> basicByConvertedEnum;

	private Map<String, SimpleComponent> componentByBasic;
	private Map<SimpleComponent, String> basicByComponent;

	private Map<String, SimpleEntity> oneToManyByBasic;
	private Map<SimpleEntity, String> basicByOneToMany;

	private Map<String, SimpleEntity> manyToManyByBasic;
	private Map<String, SimpleComponent> componentByBasicOrdered;

	private SortedMap<String, SimpleEntity> sortedManyToManyByBasic;
	private SortedMap<String, SimpleEntity> sortedManyToManyByBasicWithComparator;
	private SortedMap<String, SimpleEntity> sortedManyToManyByBasicWithSortNaturalByDefault;

	public EntityOfMaps() {
	}

	public EntityOfMaps(Integer id, String name) {
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


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basicByBasic

	@ElementCollection
	public Map<String, String> getBasicByBasic() {
		return basicByBasic;
	}

	public void setBasicByBasic(Map<String, String> basicByBasic) {
		this.basicByBasic = basicByBasic;
	}

	public void addBasicByBasic(String key, String val) {
		if ( basicByBasic == null ) {
			basicByBasic = new HashMap<>();
		}
		basicByBasic.put( key, val );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sortedBasicByBasic

	@ElementCollection
	@SortNatural
	public SortedMap<String, String> getSortedBasicByBasic() {
		return sortedBasicByBasic;
	}

	public void setSortedBasicByBasic(SortedMap<String, String> sortedBasicByBasic) {
		this.sortedBasicByBasic = sortedBasicByBasic;
	}

	public void addSortedBasicByBasic(String key, String val) {
		if ( sortedBasicByBasic == null ) {
			sortedBasicByBasic = new TreeMap<>();
		}
		sortedBasicByBasic.put( key, val );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sortedBasicByBasicWithComparator

	@ElementCollection
	@SortComparator( SimpleBasicSortComparator.class )
	public SortedMap<String, String> getSortedBasicByBasicWithComparator() {
		return sortedBasicByBasicWithComparator;
	}

	public void setSortedBasicByBasicWithComparator(SortedMap<String, String> sortedBasicByBasicWithComparator) {
		this.sortedBasicByBasicWithComparator = sortedBasicByBasicWithComparator;
	}

	public void addSortedBasicByBasicWithComparator(String key, String val) {
		if ( sortedBasicByBasicWithComparator == null ) {
			sortedBasicByBasicWithComparator = new TreeMap<>();
		}
		sortedBasicByBasicWithComparator.put( key, val );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sortedBasicByBasicWithSortNaturalByDefault

	@ElementCollection
	public SortedMap<String, String> getSortedBasicByBasicWithSortNaturalByDefault() {
		return sortedBasicByBasicWithSortNaturalByDefault;
	}

	public void setSortedBasicByBasicWithSortNaturalByDefault(SortedMap<String, String> sortedBasicByBasicWithSortNaturalByDefault) {
		this.sortedBasicByBasicWithSortNaturalByDefault = sortedBasicByBasicWithSortNaturalByDefault;
	}

	public void addSortedBasicByBasicWithSortNaturalByDefault(String key, String val) {
		if ( sortedBasicByBasicWithSortNaturalByDefault == null ) {
			sortedBasicByBasicWithSortNaturalByDefault = new TreeMap<>();
		}
		sortedBasicByBasicWithSortNaturalByDefault.put( key, val );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basicByEnum

	@ElementCollection
	@MapKeyEnumerated
	public Map<EnumValue, String> getBasicByEnum() {
		return basicByEnum;
	}

	public void setBasicByEnum(Map<EnumValue, String> basicByEnum) {
		this.basicByEnum = basicByEnum;
	}

	public void addBasicByEnum(EnumValue key, String val) {
		if ( basicByEnum == null ) {
			basicByEnum = new HashMap<>();
		}
		basicByEnum.put( key, val );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basicByConvertedEnum

	@ElementCollection
	@Convert(attributeName = "key", converter = EnumValueConverter.class)
	public Map<EnumValue, String> getBasicByConvertedEnum() {
		return basicByConvertedEnum;
	}

	public void setBasicByConvertedEnum(Map<EnumValue, String> basicByConvertedEnum) {
		this.basicByConvertedEnum = basicByConvertedEnum;
	}

	public void addBasicByConvertedEnum(EnumValue key, String value) {
		if ( basicByConvertedEnum == null ) {
			basicByConvertedEnum = new HashMap<>();
		}
		basicByConvertedEnum.put( key, value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// componentByBasic

	@ElementCollection
	public Map<String, SimpleComponent> getComponentByBasic() {
		return componentByBasic;
	}

	public void setComponentByBasic(Map<String, SimpleComponent> componentByBasic) {
		this.componentByBasic = componentByBasic;
	}

	public void addComponentByBasic(String key, SimpleComponent value) {
		if ( componentByBasic == null ) {
			componentByBasic = new HashMap<>();
		}
		componentByBasic.put( key, value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basicByComponent

	@ElementCollection
	public Map<SimpleComponent, String> getBasicByComponent() {
		return basicByComponent;
	}

	public void setBasicByComponent(Map<SimpleComponent, String> basicByComponent) {
		this.basicByComponent = basicByComponent;
	}

	public void addBasicByComponent(SimpleComponent key, String value) {
		if ( basicByComponent == null ) {
			basicByComponent = new HashMap<>();
		}
		basicByComponent.put( key, value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// oneToManyByBasic

	@OneToMany
	@JoinColumn
	public Map<String, SimpleEntity> getOneToManyByBasic() {
		return oneToManyByBasic;
	}

	public void setOneToManyByBasic(Map<String, SimpleEntity> oneToManyByBasic) {
		this.oneToManyByBasic = oneToManyByBasic;
	}

	public void addOneToManyByBasic(String key, SimpleEntity value) {
		if ( oneToManyByBasic == null ) {
			oneToManyByBasic = new HashMap<>();
		}
		oneToManyByBasic.put( key, value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// basicByOneToMany

	@ElementCollection
	public Map<SimpleEntity, String> getBasicByOneToMany() {
		return basicByOneToMany;
	}

	public void setBasicByOneToMany(Map<SimpleEntity, String> basicByOneToMany) {
		this.basicByOneToMany = basicByOneToMany;
	}

	public void addOneToManyByBasic(SimpleEntity key, String val) {
		if ( basicByOneToMany == null ) {
			basicByOneToMany = new HashMap<>();
		}
		basicByOneToMany.put( key, val );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// manyToManyByBasic

	@ManyToMany
	public Map<String, SimpleEntity> getManyToManyByBasic() {
		return manyToManyByBasic;
	}

	public void setManyToManyByBasic(Map<String, SimpleEntity> manyToManyByBasic) {
		this.manyToManyByBasic = manyToManyByBasic;
	}

	public void addManyToManyByComponent(String key, SimpleEntity value) {
		if ( manyToManyByBasic == null ) {
			manyToManyByBasic = new HashMap<>();
		}
		manyToManyByBasic.put( key, value );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// componentByBasicOrdered

	// NOTE : effectively the same as a natural-sorted map in terms of reading

	@ElementCollection
	@MapKeyColumn( name = "ordered_component_key")
	@OrderBy( clause = "ordered_component_key, ordered_component_key" )
	public Map<String, SimpleComponent> getComponentByBasicOrdered() {
		return componentByBasicOrdered;
	}

	public void setComponentByBasicOrdered(Map<String, SimpleComponent> componentByBasicOrdered) {
		this.componentByBasicOrdered = componentByBasicOrdered;
	}

	public void addComponentByBasicOrdered(String key, SimpleComponent value) {
		if ( componentByBasicOrdered == null ) {
			componentByBasicOrdered = new LinkedHashMap<>();
		}
		componentByBasicOrdered.put( key, value );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sortedManyToManyByBasic

	@ManyToMany
	@SortNatural
	public SortedMap<String, SimpleEntity> getSortedManyToManyByBasic() {
		return sortedManyToManyByBasic;
	}

	public void setSortedManyToManyByBasic(SortedMap<String, SimpleEntity> sortedManyToManyByBasic) {
		this.sortedManyToManyByBasic = sortedManyToManyByBasic;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sortedManyToManyByBasicWithComparator

	@ManyToMany
	@SortComparator( SimpleBasicSortComparator.class )
	public SortedMap<String, SimpleEntity> getSortedManyToManyByBasicWithComparator() {
		return sortedManyToManyByBasicWithComparator;
	}

	public void setSortedManyToManyByBasicWithComparator(SortedMap<String, SimpleEntity> sortedManyToManyByBasicWithComparator) {
		this.sortedManyToManyByBasicWithComparator = sortedManyToManyByBasicWithComparator;
	}

	public void addSortedManyToManyByBasicWithComparator(String key, SimpleEntity value) {
		if ( sortedManyToManyByBasicWithComparator == null ) {
			sortedManyToManyByBasicWithComparator = new TreeMap<>();
		}
		sortedManyToManyByBasicWithComparator.put( key, value );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// sortedManyToManyByBasicWithSortNaturalByDefault

	@ManyToMany
	public SortedMap<String, SimpleEntity> getSortedManyToManyByBasicWithSortNaturalByDefault() {
		return sortedManyToManyByBasicWithSortNaturalByDefault;
	}

	public void setSortedManyToManyByBasicWithSortNaturalByDefault(SortedMap<String, SimpleEntity> sortedManyToManyByBasicWithSortNaturalByDefault) {
		this.sortedManyToManyByBasicWithSortNaturalByDefault = sortedManyToManyByBasicWithSortNaturalByDefault;
	}

	public void addSortedManyToManyByBasicWithSortNaturalByDefault(String key, SimpleEntity value) {
		if ( sortedManyToManyByBasicWithSortNaturalByDefault == null ) {
			sortedManyToManyByBasicWithSortNaturalByDefault = new TreeMap<>();
		}
		sortedManyToManyByBasicWithSortNaturalByDefault.put( key, value );
	}

}
