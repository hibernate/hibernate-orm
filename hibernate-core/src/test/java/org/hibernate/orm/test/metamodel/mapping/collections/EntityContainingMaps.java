/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.metamodel.mapping.collections;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@Entity
@Table(name = "entity_containing_maps")
public class EntityContainingMaps {
	private Integer id;
	private String name;


	private Map<String,String> basicByBasic;
	private Map<EnumValue,String> basicByEnum;
	private Map<EnumValue,String> basicByConvertedEnum;

	private Map<String,SomeStuff> someStuffByBasic;
	private Map<SomeStuff, String> basicBySomeStuff;

	private Map<String,SimpleEntity> oneToManyByBasic;
	private Map<SimpleEntity,String> basicByOneToMany;

	private Map<String,SimpleEntity> manyToManyByBasic;

	public EntityContainingMaps() {
	}

	public EntityContainingMaps(Integer id, String name) {
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
	@MapKeyColumn( name = "map_key" )
	@Column( name = "map_val")
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

	@ElementCollection
	public Map<String, SomeStuff> getSomeStuffByBasic() {
		return someStuffByBasic;
	}

	public void setSomeStuffByBasic(Map<String, SomeStuff> someStuffByBasic) {
		this.someStuffByBasic = someStuffByBasic;
	}

	public void addSomeStuffByBasic(String key, SomeStuff value) {
		if ( someStuffByBasic == null ) {
			someStuffByBasic = new HashMap<>();
		}
		someStuffByBasic.put( key, value );
	}

	@ElementCollection
	public Map<SomeStuff, String> getBasicBySomeStuff() {
		return basicBySomeStuff;
	}

	public void setBasicBySomeStuff(Map<SomeStuff, String> basicBySomeStuff) {
		this.basicBySomeStuff = basicBySomeStuff;
	}

	public void addBasicBySomeStuff(SomeStuff key, String val) {
		if ( basicBySomeStuff == null ) {
			basicBySomeStuff = new HashMap<>();
		}
		basicBySomeStuff.put( key, val );
	}

	@OneToMany
	public Map<String, SimpleEntity> getOneToManyByBasic() {
		return oneToManyByBasic;
	}

	public void setOneToManyByBasic(Map<String, SimpleEntity> oneToManyByBasic) {
		this.oneToManyByBasic = oneToManyByBasic;
	}

	public void addOneToManyByBasic(String key, SimpleEntity val) {
		if ( oneToManyByBasic == null ) {
			oneToManyByBasic = new HashMap<>();
		}
		oneToManyByBasic.put( key, val );
	}

	// todo (6.0) : add support for using an entity as map key
	//		see `org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper#interpretMapKey`
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

	@ManyToMany
	@CollectionTable( name = "m2m_by_basic" )
	public Map<String, SimpleEntity> getManyToManyByBasic() {
		return manyToManyByBasic;
	}

	public void setManyToManyByBasic(Map<String, SimpleEntity> manyToManyByBasic) {
		this.manyToManyByBasic = manyToManyByBasic;
	}

	public void addManyToManyByBasic(String key, SimpleEntity val) {
		if ( manyToManyByBasic == null ) {
			manyToManyByBasic = new HashMap<>();
		}
		manyToManyByBasic.put( key, val );
	}
}
