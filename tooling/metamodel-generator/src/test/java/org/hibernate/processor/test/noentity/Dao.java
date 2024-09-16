/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.noentity;

import jakarta.persistence.TypedQuery;
import org.hibernate.annotations.processing.HQL;

public interface Dao {
	@HQL("select upper('Hibernate')")
	TypedQuery<String> getName();
}
