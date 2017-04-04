/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;
import org.hibernate.FetchMode;

/**
 * Any mapping with an outer-join attribute
 * @author Gavin King
 */
public interface Fetchable {
	public FetchMode getFetchMode();
	public void setFetchMode(FetchMode joinedFetch);
	public boolean isLazy();
	public void setLazy(boolean lazy);
}
