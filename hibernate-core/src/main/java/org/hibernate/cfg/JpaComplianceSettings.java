/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/**
 * @author Steve Ebersole
 */
public interface JpaComplianceSettings {
	/**
	 * Specifies a default value for all {@link org.hibernate.jpa.spi.JpaCompliance}
	 * flags. Each individual flag may still be overridden by explicitly specifying
	 * its specific configuration property.
	 *
	 * @settingDefault {@code true} with JPA bootstrapping; {@code false} otherwise.
	 *
	 * @see #JPA_TRANSACTION_COMPLIANCE
	 * @see #JPA_QUERY_COMPLIANCE
	 * @see #JPA_ORDER_BY_MAPPING_COMPLIANCE
	 * @see #JPA_CLOSED_COMPLIANCE
	 * @see #JPA_PROXY_COMPLIANCE
	 * @see #JPA_CACHING_COMPLIANCE
	 * @see #JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE
	 * @see #JPA_LOAD_BY_ID_COMPLIANCE
	 *
	 * @since 6.0
	 */
	String JPA_COMPLIANCE = "hibernate.jpa.compliance";

	/**
	 * When enabled, specifies that the Hibernate {@link org.hibernate.Transaction}
	 * should behave according to the semantics defined by the JPA specification for
	 * an {@link jakarta.persistence.EntityTransaction}.
	 *
	 * @settingDefault {@link #JPA_COMPLIANCE}
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaTransactionComplianceEnabled()
	 * @see org.hibernate.boot.SessionFactoryBuilder#enableJpaTransactionCompliance(boolean)
	 *
	 * @since 5.3
	 */
	String JPA_TRANSACTION_COMPLIANCE = "hibernate.jpa.compliance.transaction";

	/**
	 * Controls whether Hibernate’s handling of {@link jakarta.persistence.Query}
	 * (JPQL, Criteria and native) should strictly follow the requirements defined
	 * in the Jakarta Persistence specification, both in terms of JPQL validation
	 * and behavior of {@link jakarta.persistence.Query} method implementations.
	 *
	 * @settingDefault {@link #JPA_COMPLIANCE}
	 *
	 * @apiNote When disabled, allows the many useful features of HQL
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaQueryComplianceEnabled()
	 * @see org.hibernate.boot.SessionFactoryBuilder#enableJpaQueryCompliance(boolean)
	 *
	 * @since 5.3
	 */
	String JPA_QUERY_COMPLIANCE = "hibernate.jpa.compliance.query";

	/**
	 * @deprecated No longer has any effect.  Since 7.0 (and removal of save/update processing),
	 * Hibernate always cascades {@linkplain  jakarta.persistence.CascadeType#PERSIST PERSIST}
	 */
	@Deprecated
	String JPA_CASCADE_COMPLIANCE = "hibernate.jpa.compliance.cascade";

	/**
	 * JPA specifies that items occurring in {@link jakarta.persistence.OrderBy}
	 * lists must be references to entity attributes, whereas Hibernate, by default,
	 * allows more complex expressions.
	 *
	 * @apiNote If enabled, an exception is thrown for items which are not entity attribute
	 * references.
	 *
	 * @settingDefault {@link #JPA_COMPLIANCE}
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaOrderByMappingComplianceEnabled()
	 * @see org.hibernate.boot.SessionFactoryBuilder#enableJpaOrderByMappingCompliance(boolean)
	 *
	 * @since 6.0
	 */
	String JPA_ORDER_BY_MAPPING_COMPLIANCE	= "hibernate.jpa.compliance.orderby";

	/**
	 * JPA specifies that an {@link IllegalStateException} must be thrown by
	 * {@link jakarta.persistence.EntityManager#close()} and
	 * {@link jakarta.persistence.EntityManagerFactory#close()} if the object has
	 * already been closed. By default, Hibernate treats any additional call to
	 * {@code close()} as a noop.
	 *
	 * @apiNote When enabled, this setting forces Hibernate to throw an exception if
	 * {@code close()} is called on an instance that was already closed.
	 *
	 * @settingDefault {@link #JPA_COMPLIANCE}
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaClosedComplianceEnabled()
	 * @see org.hibernate.boot.SessionFactoryBuilder#enableJpaClosedCompliance(boolean)
	 *
	 * @since 5.3
	 */
	String JPA_CLOSED_COMPLIANCE = "hibernate.jpa.compliance.closed";

	/**
	 * The JPA specification insists that an
	 * {@link jakarta.persistence.EntityNotFoundException} must be thrown whenever
	 * an uninitialized entity proxy with no corresponding row in the database is
	 * accessed. For most programs, this results in many completely unnecessary
	 * round trips to the database.
	 * <p>
	 * Traditionally, Hibernate does not initialize an entity proxy when its
	 * identifier attribute is accessed, since the identifier value is already
	 * known and held in the proxy instance. This behavior saves the round trip
	 * to the database.
	 *
	 * @apiNote When enabled, this setting forces Hibernate to initialize the entity proxy
	 * when its identifier is accessed. Clearly, this setting is not recommended.
	 *
	 * @settingDefault {@link #JPA_COMPLIANCE}
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaProxyComplianceEnabled()
	 *
	 * @since 5.2.13
	 */
	String JPA_PROXY_COMPLIANCE = "hibernate.jpa.compliance.proxy";

	/**
	 * By default, Hibernate uses second-level cache invalidation for entities
	 * with {@linkplain jakarta.persistence.SecondaryTable secondary tables}
	 * in order to avoid the possibility of inconsistent cached data in the
	 * case where different transactions simultaneously update different table
	 * rows corresponding to the same entity instance.
	 * <p>
	 * The Jakarta Persistence TCK, requires that entities with secondary
	 * tables be immediately cached in the second-level cache rather than
	 * invalidated and re-cached on a subsequent read.
	 *
	 * @settingDefault {@link #JPA_COMPLIANCE}
	 *
	 * @apiNote Hibernate's default behavior here is safer and more careful
	 * than the behavior mandated by the TCK but YOLO
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isJpaCacheComplianceEnabled()
	 * @see org.hibernate.persister.entity.AbstractEntityPersister#isCacheInvalidationRequired()
	 *
	 * @since 5.3
	 */
	String JPA_CACHING_COMPLIANCE = "hibernate.jpa.compliance.caching";

	/**
	 * Determines whether the scope of any identifier generator name specified
	 * via {@link jakarta.persistence.TableGenerator#name()} or
	 * {@link jakarta.persistence.SequenceGenerator#name()} is considered global
	 * to the persistence unit, or local to the entity in which identifier generator
	 * is defined.
	 *
	 * @apiNote If enabled, the name will be considered globally scoped, and so the existence
	 * of two different generators with the same name will be considered a collision,
	 * and will result in an exception during bootstrap.
	 *
	 * @settingDefault {@link #JPA_COMPLIANCE}
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isGlobalGeneratorScopeEnabled()
	 *
	 * @since 5.2.17
	 */
	String JPA_ID_GENERATOR_GLOBAL_SCOPE_COMPLIANCE = "hibernate.jpa.compliance.global_id_generators";

	/**
	 * Determines if an identifier value passed to
	 * {@link jakarta.persistence.EntityManager#find} or
	 * {@link jakarta.persistence.EntityManager#getReference} may be
	 * {@linkplain  org.hibernate.type.descriptor.java.JavaType#coerce coerced} to
	 * the identifier type declared by the entity. For example, an {@link Integer}
	 * argument might be widened to {@link Long}.
	 *
	 * @settingDefault {@link #JPA_COMPLIANCE}
	 *
	 * @apiNote When enabled, coercion is disallowed, as required by the JPA specification.
	 * Hibernate's default (here non-compliant) behavior is to allow the coercion.
	 *
	 * @see org.hibernate.jpa.spi.JpaCompliance#isLoadByIdComplianceEnabled()
	 *
	 * @since 6.0
	 */
	String JPA_LOAD_BY_ID_COMPLIANCE = "hibernate.jpa.compliance.load_by_id";

	/**
	 * @deprecated Prefer {@link #JPA_QUERY_COMPLIANCE}
	 */
	@Deprecated
	String JPAQL_STRICT_COMPLIANCE= "hibernate.query.jpaql_strict_compliance";
}
