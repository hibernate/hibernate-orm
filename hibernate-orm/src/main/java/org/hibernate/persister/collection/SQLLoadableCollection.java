/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection;


public interface SQLLoadableCollection extends QueryableCollection {

	public abstract String[] getCollectionPropertyColumnAliases(String propertyName, String string);
	
	public abstract String getIdentifierColumnName();
	
}
