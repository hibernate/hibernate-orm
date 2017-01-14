/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.List;

import org.hibernate.sqm.domain.SqmSingularAttribute;

/**
 * @author Steve Ebersole
 */
public interface SingularAttribute<O,T> extends SqmSingularAttribute, Attribute<O,T>, javax.persistence.metamodel.SingularAttribute<O,T> {
	enum Disposition {
		ID,
		VERSION,
		NORMAL
	}

	/**
	 * Returns whether this attribute is <ul>
	 *     <li>part of an id?</li>
	 *     <li>the version attribute?</li>
	 *     <li>or a normal attribute?</li>
	 * </ul>
	 */
	Disposition getDisposition();

	List<Column> getColumns();

	boolean isNullable();
}
