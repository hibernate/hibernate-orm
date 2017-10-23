/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.enumerated.custom_mapkey;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;

import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import org.hibernate.test.annotations.enumerated.custom_types.LastNumberType;
import org.hibernate.test.annotations.enumerated.enums.Common;
import org.hibernate.test.annotations.enumerated.enums.FirstLetter;
import org.hibernate.test.annotations.enumerated.enums.LastNumber;

/**
 * @author Janario Oliveira
 */
@Entity
@TypeDef(typeClass = LastNumberType.class, defaultForType = LastNumber.class)
public class EntityMapEnum {
	@Id
	@GeneratedValue
	int id;

	@ElementCollection
	Map<Common, String> ordinalMap = new HashMap<Common, String>();
	@ElementCollection
	@MapKeyEnumerated(EnumType.STRING)
	Map<Common, String> stringMap = new HashMap<Common, String>();
	@ElementCollection
	@MapKeyType(@Type(type = "org.hibernate.test.annotations.enumerated.custom_types.FirstLetterType"))
	Map<FirstLetter, String> firstLetterMap = new HashMap<FirstLetter, String>();
	@ElementCollection
	Map<LastNumber, String> lastNumberMap = new HashMap<LastNumber, String>();
	@MapKeyEnumerated(EnumType.STRING)
	@ElementCollection
	@CollectionTable(name = "overridingMap")
	@MapKeyColumn(name = "overridingMap_key")
	Map<LastNumber, String> explicitOverridingImplicitMap = new HashMap<LastNumber, String>();
}
