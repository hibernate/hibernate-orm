/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping.spi;


/**
 * JAXB binding interface for association attributes (to-one and plural mappings)
 *
 * @author Steve Ebersole
 */
public interface JaxbAssociationAttribute extends JaxbPersistentAttribute, JaxbFetchableAttribute {
	JaxbJoinTableImpl getJoinTable();
	void setJoinTable(JaxbJoinTableImpl value);

	JaxbCascadeTypeImpl getCascade();
	void setCascade(JaxbCascadeTypeImpl value);

	String getTargetEntity();
	void setTargetEntity(String value);
}
