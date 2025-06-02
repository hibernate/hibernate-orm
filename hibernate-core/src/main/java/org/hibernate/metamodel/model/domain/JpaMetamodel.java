/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityGraph;

import jakarta.persistence.metamodel.Metamodel;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Extensions to the JPA-defined {@linkplain Metamodel metamodel} of
 * persistent Java types.
 *
 * @apiNote This is an incubating API. Its name and package may change.
 *
 * @see MappingMetamodel
 *
 * @since 6.0
 * @author Steve Ebersole
 */
@Incubating
public interface JpaMetamodel extends Metamodel {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Extended features

	/**
	 * Access to a managed type through its name
	 */
	<X> ManagedDomainType<X> managedType(String typeName);

	/**
	 * Access to an entity supporting Hibernate's entity-name feature
	 */
	EntityDomainType<?> entity(String entityName);

	/**
	 * Access to an embeddable type from FQN
	 */
	EmbeddableDomainType<?> embeddable(String embeddableName);

	/**
	 * Specialized handling for resolving entity-name references in
	 * an HQL query
	 */
	<X> EntityDomainType<X> getHqlEntityReference(String entityName);

	/**
	 * Specialized handling for resolving entity-name references in
	 * an HQL query
	 */
	<X> EntityDomainType<X> resolveHqlEntityReference(String entityName);

	/**
	 * Same as {@link #managedType(Class)} except {@code null} is returned rather
	 * than throwing an exception
	 */
	@Nullable <X> ManagedDomainType<X> findManagedType(Class<X> cls);

	/**
	 * Same as {@link #entity(Class)} except {@code null} is returned rather
	 * than throwing an exception
	 */
	@Nullable <X> EntityDomainType<X> findEntityType(Class<X> cls);

	/**
	 * Same as {@link #embeddable(Class)} except {@code null} is returned rather
	 * than throwing an exception
	 */
	@Nullable <X> EmbeddableDomainType<X> findEmbeddableType(Class<X> cls);

	/**
	 * Same as {@link #managedType(String)} except {@code null} is returned rather
	 * than throwing an exception
	 */
	@Nullable <X> ManagedDomainType<X> findManagedType(@Nullable String typeName);

	/**
	 * Same as {@link #entity(String)} except {@code null} is returned rather
	 * than throwing an exception
	 */
	@Nullable EntityDomainType<?> findEntityType(@Nullable String entityName);

	/**
	 * Same as {@link #embeddable(String)} except {@code null} is returned rather
	 * than throwing an exception
	 */
	@Nullable EmbeddableDomainType<?> findEmbeddableType(@Nullable String embeddableName);

	String qualifyImportableName(String queryName);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Enumerations and Java constants (useful for interpreting HQL)

	@Nullable Set<String> getEnumTypesForValue(String enumValue);

	EnumJavaType<?> getEnumType(String className);

	<E extends Enum<E>> E enumValue(EnumJavaType<E> enumType, String enumValueName);

	JavaType<?> getJavaConstantType(String className, String fieldName);

	<T> T getJavaConstant(String className, String fieldName);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Covariant returns

	@Override
	<X> ManagedDomainType<X> managedType(Class<X> cls);

	@Override
	<X> EntityDomainType<X> entity(Class<X> cls);

	@Override
	<X> EmbeddableDomainType<X> embeddable(Class<X> cls);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Entity graphs

	EntityGraph<?> findEntityGraphByName(String name);

	<T> List<? extends EntityGraph<? super T>> findEntityGraphsByJavaType(Class<T> entityClass);

	<T> Map<String, EntityGraph<? extends T>> getNamedEntityGraphs(Class<T> entityType);
}
