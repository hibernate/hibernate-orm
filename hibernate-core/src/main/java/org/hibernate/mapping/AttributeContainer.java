/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

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
	 * Add a property to this {@link PersistentClass} or {@link Join}.
	 */
	void addProperty(Property attribute);
}
