/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.util;

import java.util.List;
import java.util.Map;

/**
 * @author Hardy Ferentschik
 */
public final class Constants {
	// we are trying to to reference jpa annotations directly
	public static final String ENTITY = "jakarta.persistence.Entity";
	public static final String MAPPED_SUPERCLASS = "jakarta.persistence.MappedSuperclass";
	public static final String EMBEDDABLE = "jakarta.persistence.Embeddable";
	public static final String ID = "jakarta.persistence.Id";
	public static final String EMBEDDED_ID = "jakarta.persistence.EmbeddedId";
	public static final String TRANSIENT = "jakarta.persistence.Transient";
	public static final String BASIC = "jakarta.persistence.Basic";
	public static final String ONE_TO_ONE = "jakarta.persistence.OneToOne";
	public static final String ONE_TO_MANY = "jakarta.persistence.OneToMany";
	public static final String MANY_TO_ONE = "jakarta.persistence.ManyToOne";
	public static final String MANY_TO_MANY = "jakarta.persistence.ManyToMany";
	public static final String MAP_KEY_CLASS = "jakarta.persistence.MapKeyClass";
	public static final String ELEMENT_COLLECTION = "jakarta.persistence.ElementCollection";
	public static final String ACCESS = "jakarta.persistence.Access";
	public static final String MAP_ATTRIBUTE = "jakarta.persistence.metamodel.MapAttribute";
	public static final String CONVERT = "jakarta.persistence.Convert";
	public static final String HIBERNATE_TYPE = "org.hibernate.annotations.Type";

	public static final Map<String, String> COLLECTIONS = allCollectionTypes();

	private static java.util.Map<String, String> allCollectionTypes() {
		Map<String, String> map = new java.util.HashMap<>();
		map.put( java.util.Collection.class.getName(), "jakarta.persistence.metamodel.CollectionAttribute" );
		map.put( java.util.Set.class.getName(), "jakarta.persistence.metamodel.SetAttribute" );
		map.put( java.util.List.class.getName(), "jakarta.persistence.metamodel.ListAttribute" );
		map.put( java.util.Map.class.getName(), "jakarta.persistence.metamodel.MapAttribute" );

		// Hibernate also supports the SortedSet and SortedMap interfaces
		map.put( java.util.SortedSet.class.getName(), "jakarta.persistence.metamodel.SetAttribute" );
		map.put( java.util.SortedMap.class.getName(), "jakarta.persistence.metamodel.MapAttribute" );
		return java.util.Collections.unmodifiableMap( map );
	}

	public static final List<String> BASIC_TYPES = allBasicTypes();

	private static java.util.List<String> allBasicTypes() {
		java.util.ArrayList<String> strings = new java.util.ArrayList<>();
		strings.add( java.lang.String.class.getName() );
		strings.add( java.lang.Boolean.class.getName() );
		strings.add( java.lang.Byte.class.getName() );
		strings.add( java.lang.Character.class.getName() );
		strings.add( java.lang.Short.class.getName() );
		strings.add( java.lang.Integer.class.getName() );
		strings.add( java.lang.Long.class.getName() );
		strings.add( java.lang.Float.class.getName() );
		strings.add( java.lang.Double.class.getName() );
		strings.add( java.math.BigInteger.class.getName() );
		strings.add( java.math.BigDecimal.class.getName() );
		strings.add( java.util.Date.class.getName() );
		strings.add( java.util.Calendar.class.getName() );
		strings.add( java.sql.Date.class.getName() );
		strings.add( java.sql.Time.class.getName() );
		strings.add( java.sql.Timestamp.class.getName() );
		strings.add( java.sql.Blob.class.getName() );
		return java.util.Collections.unmodifiableList( strings );
	}

	public static final List<String> BASIC_ARRAY_TYPES = allBasicArrayTypes();

	private static java.util.List<String> allBasicArrayTypes() {
		java.util.ArrayList<String> strings = new java.util.ArrayList<>();
		strings.add( java.lang.Character.class.getName() );
		strings.add( java.lang.Byte.class.getName() );
		return java.util.Collections.unmodifiableList( strings );
	}

	public static final String PATH_SEPARATOR = "/";

	private Constants() {
	}
}
