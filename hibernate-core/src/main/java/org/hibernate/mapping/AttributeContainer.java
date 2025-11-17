/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;

/**
 * Identifies a mapping model object which may have {@linkplain Property attributes}
 * (fields or properties). Abstracts over {@link PersistentClass} and {@link Join}.
 *
 * @apiNote This model makes sense in {@code hbm.xml} mappings where a
 *          {@code <property/>} element may occur as a child of a {@code <join/>}.
 *          In annotations, and in {@code orm.xml}, a property cannot be said to
 *          itself belong to a secondary table, instead its columns are mapped to
 *          the table explicitly. In fact, the old {@code hbm.xml} model was more
 *          natural when it came to handling {@code <column/>} and especially
 *          {@code <formula/>} mappings in secondary tables. There was no need to
 *          repetitively write {@code @Column(table="secondary")}. Granted, it does
 *          sound strange to say that a Java property "belongs" to a secondary table.
 *
 * @author Steve Ebersole
 */
public interface AttributeContainer {
	/**
	 * Add an attribute to this {@link PersistentClass} or {@link Join}.
	 */
	void addProperty(Property property);

	/**
	 * Determine if the given attribute belongs to this container.
	 */
	boolean contains(Property property);

	/**
	 * Get the attribute with the given name belonging to this container.
	 * @param propertyName the name of an attribute
	 * @throws MappingException if there is no attribute with the given name
	 * @since 7.2
	 */
	Property getProperty(String propertyName) throws MappingException;

	/**
	 * The {@link Table} with the columns mapped by attributes belonging
	 * to this container.
	 */
	Table getTable();
}
