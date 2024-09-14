/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.interfaceproxy;


/**
 * @author Gavin King
 */
public interface Folder extends Item {
	/**
	 * @return Returns the parent.
	 */
	public Folder getParent();

	/**
	 * @param parent The parent to set.
	 */
	public void setParent(Folder parent);
}
