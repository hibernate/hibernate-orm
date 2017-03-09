/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface ListAttribute<O,E> extends PluralPersistentAttribute<O,List<E>,E>,
		javax.persistence.metamodel.ListAttribute<O,E> {
}
