/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

import org.hibernate.Incubating;

/**
 * A set of rules for determining the {@linkplain PhysicalNamingStrategy logical name}
 * of a mapped relational database object when the mapping for an element of the Java
 * domain model is not explicitly specified, neither in annotations of the Java code,
 * nor in an XML-based mapping document.
 * <p>
 * For example, if a Java class annotated {@link jakarta.persistence.Entity @Entity}
 * has no {@link jakarta.persistence.Table @Table} annotation, then
 * {@link #determinePrimaryTableName(ImplicitEntityNameSource) determinePrimaryTableName}
 * is called with an {@link ImplicitEntityNameSource} providing access to information
 * about the Java class and its {@link jakarta.persistence.Entity#name() entity name}.
 * <p>
 * On the other hand, when a logical name <em>is</em> explicitly specified, for example,
 * using {@link jakarta.persistence.Table#name() @Table} to specify the table name,
 * or {@link jakarta.persistence.Column#name() @Column} to specify a column name, the
 * {@code ImplicitNamingStrategy} is not called and has no opportunity to intervene in
 * the determination of the logical name.
 * <p>
 * However, a further level of processing is applied to the resulting logical names by
 * a {@link PhysicalNamingStrategy} in order to determine the "finally final" physical
 * names in the relational database schema.
 * <p>
 * Whenever reasonable, the use of a custom {@code ImplicitNamingStrategy} is highly
 * recommended in preference to tedious and repetitive explicit table and column name
 * mappings. It's anticipated that most projects using Hibernate will feature a custom
 * implementation of {@code ImplicitNamingStrategy}.
 * <p>
 * An {@code ImplicitNamingStrategy} may be selected using the configuration property
 * {@value org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY}.
 *
 * @apiNote The method names here mostly favor the JPA terminology,
 *          for example, "secondary table" rather than "join".
 *
 * @see PhysicalNamingStrategy
 * @see org.hibernate.cfg.Configuration#setImplicitNamingStrategy(ImplicitNamingStrategy)
 * @see org.hibernate.boot.MetadataBuilder#applyImplicitNamingStrategy(ImplicitNamingStrategy)
 * @see org.hibernate.cfg.AvailableSettings#IMPLICIT_NAMING_STRATEGY
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ImplicitNamingStrategy {

	/**
	 * Determine the implicit name of an entity's primary table.
	 *
	 * @param source The source information
	 *
	 * @return The implicit table name.
	 */
	Identifier determinePrimaryTableName(ImplicitEntityNameSource source);

	/**
	 * Determine the name of an association join table given the source naming
	 * information, when a name is not explicitly given. This method is called
	 * for any sort of association with a join table, no matter what the logical
	 * cardinality.
	 *
	 * @param source The source information
	 *
	 * @return The implicit table name.
	 */
	Identifier determineJoinTableName(ImplicitJoinTableNameSource source);

	/**
	 * Determine the name of a collection join table given the source naming
	 * information, when a name is not explicitly given. This method is called
	 * only for {@linkplain jakarta.persistence.ElementCollection collections of
	 * basic or embeddable values}, and never for associations.
	 *
	 * @param source The source information
	 *
	 * @return The implicit table name.
	 */
	Identifier determineCollectionTableName(ImplicitCollectionTableNameSource source);


	/**
	 * Determine the {@linkplain jakarta.persistence.DiscriminatorValue discriminator}
	 * column name for the given entity when it is not explicitly specified using
	 * {@link jakarta.persistence.DiscriminatorColumn#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The implicit discriminator column name
	 */
	Identifier determineDiscriminatorColumnName(ImplicitDiscriminatorColumnNameSource source);

	/**
	 * Determine the implicit name of the {@linkplain org.hibernate.annotations.TenantId
	 * tenant identifier} column belonging to a given entity when it is not explicitly
	 * specified using {@link jakarta.persistence.Column#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The determined tenant identifier column name
	 */
	Identifier determineTenantIdColumnName(ImplicitTenantIdColumnNameSource source);

	/**
	 * Determine the name if the {@linkplain jakarta.persistence.Id identifier} column
	 * belonging to the given entity when it is not explicitly specified using
	 * {@link jakarta.persistence.Column#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The determined identifier column name
	 */
	Identifier determineIdentifierColumnName(ImplicitIdentifierColumnNameSource source);

	/**
	 * Determine the column name when it is not explicitly specified using
	 * {@link jakarta.persistence.Column#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The implicit column name.
	 */
	Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source);

	/**
	 * Determine the join column name when it is not explicitly specified using
	 * {@link jakarta.persistence.JoinColumn#name()}.
	 * <p>
	 * In {@code hbm.xml} terms, this would be a {@code <key/>} defined for a
	 * collection or the column associated with a many-to-one.
	 *
	 * @param source The source information
	 *
	 * @return The determined join column name
	 */
	Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source);

	/**
	 * Determine the primary key join column name when it is not explicitly specified
	 * using {@link jakarta.persistence.PrimaryKeyJoinColumn#name()}.
	 * <p>
	 * In {@code hbm.xml} terms, this would be a {@code <key/>} defined for a
	 * {@code <join/>} or a {@code <joined-subclass/>}.
	 *
	 * @param source The source information
	 *
	 * @return The determined column name
	 */
	Identifier determinePrimaryKeyJoinColumnName(ImplicitPrimaryKeyJoinColumnNameSource source);

	/**
	 * Determine the column name related to the discriminator portion of an
	 * {@link org.hibernate.annotations.Any} mapping when no explicit column
	 * name is given using {@link jakarta.persistence.Column#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The determined column name
	 */
	Identifier determineAnyDiscriminatorColumnName(ImplicitAnyDiscriminatorColumnNameSource source);

	/**
	 * Determine the join column name related to the key/id portion of an
	 * {@link org.hibernate.annotations.Any} mapping when no explicit join column
	 * name is given using {@link jakarta.persistence.JoinColumn#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The determined identifier column name
	 */
	Identifier determineAnyKeyColumnName(ImplicitAnyKeyColumnNameSource source);

	/**
	 * Determine the map key column name when it is not explicitly specified using
	 * {@link jakarta.persistence.MapKeyColumn#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The implicit column name.
	 */
	Identifier determineMapKeyColumnName(ImplicitMapKeyColumnNameSource source);

	/**
	 * Determine the list index column name when it is not explicitly specified using
	 * {@link jakarta.persistence.OrderColumn#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The implicit column name.
	 */
	Identifier determineListIndexColumnName(ImplicitIndexColumnNameSource source);

	/**
	 * Determine the foreign key name when it is not explicitly specified using
	 * {@link jakarta.persistence.ForeignKey#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The determined foreign key name
	 */
	Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source);

	/**
	 * Determine the unique key name when it is not explicitly specified using
	 * {@link jakarta.persistence.UniqueConstraint#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The determined foreign key name
	 */
	Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source);

	/**
	 * Determine the index name when it is not explicitly specified using
	 * {@link jakarta.persistence.Index#name()}.
	 *
	 * @param source The source information
	 *
	 * @return The determined foreign key name
	 */
	Identifier determineIndexName(ImplicitIndexNameSource source);
}
