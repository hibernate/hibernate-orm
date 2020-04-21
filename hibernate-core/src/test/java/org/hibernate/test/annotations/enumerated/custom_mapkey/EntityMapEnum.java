/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.enumerated.custom_mapkey;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;

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
