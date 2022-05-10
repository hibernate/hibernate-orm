/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.mapping;

import java.util.List;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

/**
 * JAXB binding interface for plural attributes
 *
 * @author Brett Meyer
 */
public interface CollectionAttribute extends FetchableAttribute {
	JaxbPluralFetchMode getFetchMode();

	void setFetchMode(JaxbPluralFetchMode mode);

	String getOrderBy();

	void setOrderBy(String value);

	JaxbOrderColumn getOrderColumn();

	void setOrderColumn(JaxbOrderColumn value);

	String getSort();

	void setSort(String value);

	JaxbMapKey getMapKey();

	void setMapKey(JaxbMapKey value);

	JaxbMapKeyClass getMapKeyClass();

	void setMapKeyClass(JaxbMapKeyClass value);

	TemporalType getMapKeyTemporal();

	void setMapKeyTemporal(TemporalType value);

	EnumType getMapKeyEnumerated();

	void setMapKeyEnumerated(EnumType value);

	List<JaxbAttributeOverride> getMapKeyAttributeOverride();

	List<JaxbConvert> getMapKeyConvert();

	JaxbMapKeyColumn getMapKeyColumn();

	void setMapKeyColumn(JaxbMapKeyColumn value);

	List<JaxbMapKeyJoinColumn> getMapKeyJoinColumn();

	JaxbForeignKey getMapKeyForeignKey();

	void setMapKeyForeignKey(JaxbForeignKey value);
}
