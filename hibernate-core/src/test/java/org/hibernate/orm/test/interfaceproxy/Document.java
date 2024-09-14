/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.interfaceproxy;
import java.sql.Blob;
import java.util.Calendar;

/**
 * @author Gavin King
 */
public interface Document extends Item {
	/**
	 * @return Returns the content.
	 */
	public Blob getContent();

	/**
	 * @param content The content to set.
	 */
	public void setContent(Blob content);

	public Calendar getCreated();

	public Calendar getModified();
}
