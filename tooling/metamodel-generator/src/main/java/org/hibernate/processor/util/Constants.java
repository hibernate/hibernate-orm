/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.util;

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
	public static final String EMBEDDED = "jakarta.persistence.Embedded";
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
	public static final String GENERATED_VALUE = "jakarta.persistence.GeneratedValue";
	public static final String ORDER_BY = "jakarta.persistence.OrderBy";

	public static final String NAMED_QUERY = "jakarta.persistence.NamedQuery";
	public static final String NAMED_QUERIES = "jakarta.persistence.NamedQueries";
	public static final String NAMED_NATIVE_QUERY = "jakarta.persistence.NamedNativeQuery";
	public static final String NAMED_NATIVE_QUERIES = "jakarta.persistence.NamedNativeQueries";
	public static final String SQL_RESULT_SET_MAPPING = "jakarta.persistence.SqlResultSetMapping";
	public static final String SQL_RESULT_SET_MAPPINGS = "jakarta.persistence.SqlResultSetMappings";
	public static final String NAMED_ENTITY_GRAPH = "jakarta.persistence.NamedEntityGraph";
	public static final String NAMED_ENTITY_GRAPHS = "jakarta.persistence.NamedEntityGraphs";

	public static final String TYPED_QUERY_REFERENCE = "jakarta.persistence.TypedQueryReference";
	public static final String ENTITY_GRAPH = "jakarta.persistence.EntityGraph";

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
	public static final String PATTERN = "org.hibernate.annotations.processing.Pattern";
	public static final String EXCLUDE = "org.hibernate.annotations.processing.Exclude";

	public static final String JD_REPOSITORY = "jakarta.data.repository.Repository";
	public static final String JD_QUERY = "jakarta.data.repository.Query";
	public static final String JD_FIND = "jakarta.data.repository.Find";
	public static final String JD_INSERT = "jakarta.data.repository.Insert";
	public static final String JD_UPDATE = "jakarta.data.repository.Update";
	public static final String JD_DELETE = "jakarta.data.repository.Delete";
	public static final String JD_SAVE = "jakarta.data.repository.Save";
	public static final String JD_LIMIT = "jakarta.data.Limit";
	public static final String JD_SORT = "jakarta.data.Sort";
	public static final String JD_ORDER = "jakarta.data.Order";
	public static final String JD_PAGE_REQUEST = "jakarta.data.page.PageRequest";
	public static final String JD_PAGE = "jakarta.data.page.Page";
	public static final String JD_CURSORED_PAGE = "jakarta.data.page.CursoredPage";
	public static final String BASIC_REPOSITORY = "jakarta.data.repository.BasicRepository";
	public static final String CRUD_REPOSITORY = "jakarta.data.repository.CrudRepository";
	public static final String DATA_REPOSITORY = "jakarta.data.repository.DataRepository";
	public static final String JD_ORDER_BY = "jakarta.data.repository.OrderBy";
	public static final String JD_ORDER_BY_LIST = "jakarta.data.repository.OrderBy.List";

	public static final String JD_LIFECYCLE_EVENT = "jakarta.data.event.LifecycleEvent";

	public static final String HIB_ORDER = "org.hibernate.query.Order";
	public static final String HIB_PAGE = "org.hibernate.query.Page";
	public static final String HIB_KEYED_PAGE = "org.hibernate.query.KeyedPage";
	public static final String HIB_KEYED_RESULT_LIST = "org.hibernate.query.KeyedResultList";
	public static final String HIB_SORT_DIRECTION = "org.hibernate.query.SortDirection";
	public static final String HIB_RESTRICTION = "org.hibernate.query.restriction.Restriction";
	public static final String HIB_RANGE = "org.hibernate.query.range.Range";

	public static final String CHECK_HQL = "org.hibernate.annotations.processing.CheckHQL";

	public static final String ENTITY_MANAGER = "jakarta.persistence.EntityManager";
	public static final String ENTITY_MANAGER_FACTORY = "jakarta.persistence.EntityManagerFactory";
	public static final String QUERY = "jakarta.persistence.Query";
	public static final String TYPED_QUERY = "jakarta.persistence.TypedQuery";
	public static final String HIB_QUERY = "org.hibernate.query.Query";
	public static final String HIB_SELECTION_QUERY = "org.hibernate.query.SelectionQuery";
	public static final String HIB_SESSION = "org.hibernate.Session";
	public static final String HIB_SESSION_FACTORY = "org.hibernate.SessionFactory";
	public static final String HIB_STATELESS_SESSION = "org.hibernate.StatelessSession";
	public static final String MUTINY_SESSION_FACTORY = "org.hibernate.reactive.mutiny.Mutiny.SessionFactory";
	public static final String MUTINY_SESSION = "org.hibernate.reactive.mutiny.Mutiny.Session";
	public static final String MUTINY_STATELESS_SESSION = "org.hibernate.reactive.mutiny.Mutiny.StatelessSession";
	public static final String QUARKUS_SESSION_OPERATIONS = "io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations";
	public static final String HIB_ENABLED_FETCH_PROFILE = "org.hibernate.EnabledFetchProfile";

	public static final String TUPLE = "jakarta.persistence.Tuple";

	public static final String INJECT = "jakarta.inject.Inject";
	public static final String TYPE_LITERAL = "jakarta.enterprise.util.TypeLiteral";
	public static final String EVENT = "jakarta.enterprise.event.Event";

	public static final String UNI = "io.smallrye.mutiny.Uni";
	public static final String UNI_MUTINY_SESSION = UNI + "<" + MUTINY_SESSION + ">";
	public static final String UNI_MUTINY_STATELESS_SESSION = UNI + "<" + MUTINY_STATELESS_SESSION + ">";
	public static final String UNI_INTEGER = UNI+"<java.lang.Integer>";
	public static final String UNI_VOID = UNI+"<java.lang.Void>";
	public static final String UNI_BOOLEAN = UNI+"<java.lang.Boolean>";
	public static final String BOXED_VOID = "java.lang.Void";

	public static final String SINGULAR_ATTRIBUTE = "jakarta.persistence.metamodel.SingularAttribute";
	public static final String COLLECTION_ATTRIBUTE = "jakarta.persistence.metamodel.CollectionAttribute";
	public static final String SET_ATTRIBUTE = "jakarta.persistence.metamodel.SetAttribute";
	public static final String LIST_ATTRIBUTE = "jakarta.persistence.metamodel.ListAttribute";
	public static final String MAP_ATTRIBUTE = "jakarta.persistence.metamodel.MapAttribute";

	public static final String PERSISTENCE_UNIT = "jakarta.persistence.PersistenceUnit";
	public static final String POST_CONSTRUCT = "jakarta.annotation.PostConstruct";
	public static final String PRE_DESTROY = "jakarta.annotation.PreDestroy";

	public static final String JAVA_OBJECT = "java.lang.Object";
	public static final String VOID = "java.lang.Void";
	public static final String STRING = "java.lang.String";
	public static final String BOOLEAN = "java.lang.Boolean";
	public static final String OBJECTS = "java.util.Objects";
	public static final String ITERABLE = "java.lang.Iterable";
	public static final String COLLECTION = "java.util.Collection";
	public static final String LIST = "java.util.List";
	public static final String MAP = "java.util.Map";
	public static final String SET = "java.util.Set";
	public static final String OPTIONAL = "java.util.Optional";
	public static final String STREAM = "java.util.stream.Stream";
	public static final String COLLECTORS = "java.util.stream.Collectors";

	public static final String NULLABLE = "jakarta.annotation.Nullable";
	public static final String NONNULL = "jakarta.annotation.Nonnull";
	public static final String NOT_NULL = "jakarta.validation.constraints.NotNull";

	public static final String PANACHE_ORM_REPOSITORY_BASE = "io.quarkus.hibernate.orm.panache.PanacheRepositoryBase";
	public static final String PANACHE_ORM_ENTITY_BASE = "io.quarkus.hibernate.orm.panache.PanacheEntityBase";
	public static final String PANACHE_REACTIVE_REPOSITORY_BASE = "io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase";
	public static final String PANACHE_REACTIVE_ENTITY_BASE = "io.quarkus.hibernate.reactive.panache.PanacheEntityBase";

	public static final String SPRING_OBJECT_PROVIDER = "org.springframework.beans.factory.ObjectProvider";
	public static final String SPRING_STATELESS_SESSION_PROVIDER = SPRING_OBJECT_PROVIDER + "<" + HIB_STATELESS_SESSION + ">";
	public static final String SPRING_COMPONENT = "org.springframework.stereotype.Component";

	public static final String PANACHE2_ENTITY_MARKER = "io.quarkus.hibernate.panache.PanacheEntityMarker";
	public static final String PANACHE2_MANAGED_BLOCKING_REPOSITORY_BASE = "io.quarkus.hibernate.panache.managed.blocking.PanacheManagedBlockingRepositoryBase";
	public static final String PANACHE2_STATELESS_BLOCKING_REPOSITORY_BASE = "io.quarkus.hibernate.panache.stateless.blocking.PanacheStatelessBlockingRepositoryBase";
	public static final String PANACHE2_MANAGED_REACTIVE_REPOSITORY_BASE = "io.quarkus.hibernate.panache.managed.reactive.PanacheManagedReactiveRepositoryBase";
	public static final String PANACHE2_STATELESS_REACTIVE_REPOSITORY_BASE = "io.quarkus.hibernate.panache.stateless.reactive.PanacheStatelessReactiveRepositoryBase";

	public static final Map<String, String> COLLECTIONS = Map.of(
			COLLECTION, COLLECTION_ATTRIBUTE,
			SET, SET_ATTRIBUTE,
			LIST, LIST_ATTRIBUTE,
			MAP, MAP_ATTRIBUTE,
			// Hibernate also supports the SortedSet and SortedMap interfaces
			java.util.SortedSet.class.getName(), SET_ATTRIBUTE,
			java.util.SortedMap.class.getName(), MAP_ATTRIBUTE
	);

	public static final Set<String> SESSION_TYPES =
			Set.of(
					ENTITY_MANAGER,
					HIB_SESSION,
					HIB_STATELESS_SESSION,
					MUTINY_SESSION,
					MUTINY_STATELESS_SESSION,
					UNI_MUTINY_SESSION,
					UNI_MUTINY_STATELESS_SESSION,
					SPRING_STATELESS_SESSION_PROVIDER
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
