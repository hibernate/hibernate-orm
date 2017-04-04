/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Collection related statistics
 *
 * @author Gavin King
 */
public interface CollectionStatistics extends Serializable {

	long getLoadCount();

	long getFetchCount();

	long getRecreateCount();

	long getRemoveCount();

	long getUpdateCount();
}
