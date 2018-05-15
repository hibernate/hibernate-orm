/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.xml.hbm;

/**
 * @author Emmanuel Bernard
 */
public interface A extends java.io.Serializable {
	public Integer getAId();

	public void setAId(Integer aId);

	String getDescription();

	void setDescription(String description);
}
