/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Hardy Ferentschik
 */
public final class Constants {

	// All this is to avoid loading classes we don't need to load

	public static final String ENTITY = "jakarta.persistence.Entity";
	public static final String MAPPED_SUPERCLASS = "jakarta.persistence.MappedSuperclass";
	public static final String EMBEDDABLE = "jakarta.persistence.Embeddable";
	public static final String ID = "jakarta.persistence.Id";
	public static final String ID_CLASS = "jakarta.persistence.IdClass";
	public static final String EMBEDDED_ID = "jakarta.persistence.EmbeddedId";
	public static final String NATURAL_ID = "org.hibernate.annotations.NaturalId";
	public static final String TRANSIENT = "jakarta.persistence.Transient";
	public static final String BASIC = "jakarta.persistence.Basic";
	public static final String ONE_TO_ONE = "jakarta.persistence.OneToOne";
	public static final String ONE_TO_MANY = "jakarta.persistence.OneToMany";
	public static final String MANY_TO_ONE = "jakarta.persistence.ManyToOne";
	public static final String MANY_TO_MANY = "jakarta.persistence.ManyToMany";
	public static final String MANY_TO_ANY = "org.hibernate.annotations.ManyToAny";
	public static final String MAP_KEY_CLASS = "jakarta.persistence.MapKeyClass";
	public static final String ELEMENT_COLLECTION = "jakarta.persistence.ElementCollection";
	public static final String ACCESS = "jakarta.persistence.Access";
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

	public static final String HQL = "org.hibernate.annotations.processing.HQL";
	public static final String SQL = "org.hibernate.annotations.processing.SQL";
	public static final String FIND = "org.hibernate.annotations.processing.Find";

	public static final String CHECK_HQL = "org.hibernate.annotations.processing.CheckHQL";

	public static final String ENTITY_MANAGER = "jakarta.persistence.EntityManager";
	public static final String QUERY = "jakarta.persistence.Query";
	public static final String TYPED_QUERY = "jakarta.persistence.TypedQuery";
	public static final String HIB_QUERY = "org.hibernate.query.Query";
	public static final String HIB_SELECTION_QUERY = "org.hibernate.query.SelectionQuery";
	public static final String HIB_SESSION = "org.hibernate.Session";
	public static final String HIB_STATELESS_SESSION = "org.hibernate.StatelessSession";
	public static final String MUTINY_SESSION = "org.hibernate.reactive.mutiny.Mutiny.Session";

	public static final String TUPLE = "jakarta.persistence.Tuple";

	public static final String UNI = "io.smallrye.mutiny.Uni";

	public static final String SINGULAR_ATTRIBUTE = "jakarta.persistence.metamodel.SingularAttribute";
	public static final String COLLECTION_ATTRIBUTE = "jakarta.persistence.metamodel.CollectionAttribute";
	public static final String SET_ATTRIBUTE = "jakarta.persistence.metamodel.SetAttribute";
	public static final String LIST_ATTRIBUTE = "jakarta.persistence.metamodel.ListAttribute";
	public static final String MAP_ATTRIBUTE = "jakarta.persistence.metamodel.MapAttribute";

	public static final String COLLECTION = java.util.Collection.class.getName();
	public static final String LIST = java.util.List.class.getName();
	public static final String MAP = java.util.Map.class.getName();
	public static final String SET = java.util.Set.class.getName();

	public static final Map<String, String> COLLECTIONS = Map.of(
			COLLECTION, Constants.COLLECTION_ATTRIBUTE,
			SET, Constants.SET_ATTRIBUTE,
			LIST, Constants.LIST_ATTRIBUTE,
			MAP, Constants.MAP_ATTRIBUTE,
			// Hibernate also supports the SortedSet and SortedMap interfaces
			java.util.SortedSet.class.getName(), Constants.SET_ATTRIBUTE,
			java.util.SortedMap.class.getName(), Constants.MAP_ATTRIBUTE
	);

	public static final Set<String> SESSION_TYPES =
			Set.of(
					Constants.ENTITY_MANAGER,
					Constants.HIB_SESSION,
					Constants.HIB_STATELESS_SESSION,
					Constants.MUTINY_SESSION
			);

	//TODO: this is not even an exhaustive list of built-in basic types
	//      so any logic that relies on incomplete this list is broken!
	public static final Set<String> BASIC_TYPES =  Set.of(
			String.class.getName(),
			Boolean.class.getName(),
			Byte.class.getName(),
			Character.class.getName(),
			Short.class.getName(),
			Integer.class.getName(),
			Long.class.getName(),
			Float.class.getName(),
			Double.class.getName(),
			BigInteger.class.getName(),
			BigDecimal.class.getName(),
			java.util.Date.class.getName(),
			java.util.Calendar.class.getName(),
			java.sql.Date.class.getName(),
			java.sql.Time.class.getName(),
			java.sql.Timestamp.class.getName(),
			java.sql.Blob.class.getName()
	);

	public static final List<String> BASIC_ARRAY_TYPES = List.of(
			Character.class.getName(),
			Byte.class.getName()
	);

	private Constants() {
	}
}
