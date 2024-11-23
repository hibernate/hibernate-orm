/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.jaxb.mapping.spi;

public interface AssociationAttribute extends PersistentAttribute, FetchableAttribute {

	JaxbJoinTable getJoinTable();

	void setJoinTable(JaxbJoinTable value);

	JaxbCascadeType getCascade();

	void setCascade(JaxbCascadeType value);

	String getTargetEntity();

	void setTargetEntity(String value);

}
