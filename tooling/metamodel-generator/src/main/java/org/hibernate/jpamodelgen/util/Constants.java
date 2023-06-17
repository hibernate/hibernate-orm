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
	// we are trying to reference jpa annotations directly
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

	public static final String NAMED_QUERY = "jakarta.persistence.NamedQuery";
	public static final String NAMED_QUERIES = "jakarta.persistence.NamedQueries";
	public static final String NAMED_NATIVE_QUERY = "jakarta.persistence.NamedNativeQuery";
	public static final String NAMED_NATIVE_QUERIES = "jakarta.persistence.NamedNativeQueries";
	public static final String SQL_RESULT_SET_MAPPING = "jakarta.persistence.SqlResultSetMapping";
	public static final String SQL_RESULT_SET_MAPPINGS = "jakarta.persistence.SqlResultSetMappings";
	public static final String NAMED_ENTITY_GRAPH = "jakarta.persistence.NamedEntityGraph";
	public static final String NAMED_ENTITY_GRAPHS = "jakarta.persistence.NamedEntityGraphs";

	public static final String HIB_NAMED_QUERY = "org.hibernate.annotations.NamedQuery";
	public static final String HIB_NAMED_QUERIES = "org.hibernate.annotations.NamedQueries";
	public static final String HIB_NAMED_NATIVE_QUERY = "org.hibernate.annotations.NamedNativeQuery";
	public static final String HIB_NAMED_NATIVE_QUERIES = "org.hibernate.annotations.NamedNativeQueries";
	public static final String HIB_FETCH_PROFILE = "org.hibernate.annotations.FetchProfile";
	public static final String HIB_FETCH_PROFILES = "org.hibernate.annotations.FetchProfiles";
	public static final String HIB_FILTER_DEF = "org.hibernate.annotations.FilterDef";
	public static final String HIB_FILTER_DEFS = "org.hibernate.annotations.FilterDefs";

	public static final String HQL = "org.hibernate.annotations.Hql";
	public static final String SQL = "org.hibernate.annotations.Sql";

	public static final Map<String, String> COLLECTIONS = allCollectionTypes();

	private static Map<String, String> allCollectionTypes() {
		Map<String, String> map = new java.util.HashMap<>();
		map.put( java.util.Collection.class.getName(), "jakarta.persistence.metamodel.CollectionAttribute" );
		map.put( java.util.Set.class.getName(), "jakarta.persistence.metamodel.SetAttribute" );
		map.put( List.class.getName(), "jakarta.persistence.metamodel.ListAttribute" );
		map.put( Map.class.getName(), "jakarta.persistence.metamodel.MapAttribute" );

		// Hibernate also supports the SortedSet and SortedMap interfaces
		map.put( java.util.SortedSet.class.getName(), "jakarta.persistence.metamodel.SetAttribute" );
		map.put( java.util.SortedMap.class.getName(), "jakarta.persistence.metamodel.MapAttribute" );
		return java.util.Collections.unmodifiableMap( map );
	}

	public static final List<String> BASIC_TYPES = allBasicTypes();

	private static List<String> allBasicTypes() {
		java.util.ArrayList<String> strings = new java.util.ArrayList<>();
		strings.add( String.class.getName() );
		strings.add( Boolean.class.getName() );
		strings.add( Byte.class.getName() );
		strings.add( Character.class.getName() );
		strings.add( Short.class.getName() );
		strings.add( Integer.class.getName() );
		strings.add( Long.class.getName() );
		strings.add( Float.class.getName() );
		strings.add( Double.class.getName() );
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

	private static List<String> allBasicArrayTypes() {
		java.util.ArrayList<String> strings = new java.util.ArrayList<>();
		strings.add( Character.class.getName() );
		strings.add( Byte.class.getName() );
		return java.util.Collections.unmodifiableList( strings );
	}

	public static final String PATH_SEPARATOR = "/";

	private Constants() {
	}
}
