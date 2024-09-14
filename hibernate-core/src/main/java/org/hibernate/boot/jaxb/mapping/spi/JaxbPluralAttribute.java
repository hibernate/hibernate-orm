/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

import org.hibernate.boot.internal.LimitedCollectionClassification;

import jakarta.persistence.EnumType;
import jakarta.persistence.TemporalType;

/**
 * JAXB binding interface for plural attributes
 *
 * @author Brett Meyer
 */
public interface JaxbPluralAttribute extends JaxbPersistentAttribute, JaxbLockableAttribute, JaxbStandardAttribute {
	JaxbPluralFetchModeImpl getFetchMode();
	void setFetchMode(JaxbPluralFetchModeImpl mode);

	JaxbCollectionUserTypeImpl getCollectionType();
	void setCollectionType(JaxbCollectionUserTypeImpl value);

	JaxbCollectionIdImpl getCollectionId();
	void setCollectionId(JaxbCollectionIdImpl id);


	LimitedCollectionClassification getClassification();
	void setClassification(LimitedCollectionClassification value);

	String getOrderBy();
	void setOrderBy(String value);

	JaxbOrderColumnImpl getOrderColumn();
	void setOrderColumn(JaxbOrderColumnImpl value);

	String getSort();
	void setSort(String value);

	JaxbPluralAnyMappingImpl.JaxbSortNaturalImpl getSortNatural();
	void setSortNatural(JaxbPluralAnyMappingImpl.JaxbSortNaturalImpl value);

	JaxbMapKeyImpl getMapKey();
	void setMapKey(JaxbMapKeyImpl value);

	JaxbMapKeyClassImpl getMapKeyClass();
	void setMapKeyClass(JaxbMapKeyClassImpl value);

	TemporalType getMapKeyTemporal();

	void setMapKeyTemporal(TemporalType value);

	EnumType getMapKeyEnumerated();

	void setMapKeyEnumerated(EnumType value);

	List<JaxbAttributeOverrideImpl> getMapKeyAttributeOverrides();

	List<JaxbConvertImpl> getMapKeyConverts();

	JaxbMapKeyColumnImpl getMapKeyColumn();

	void setMapKeyColumn(JaxbMapKeyColumnImpl value);

	JaxbUserTypeImpl getMapKeyType();

	void setMapKeyType(JaxbUserTypeImpl value);

	List<JaxbMapKeyJoinColumnImpl> getMapKeyJoinColumns();

	JaxbForeignKeyImpl getMapKeyForeignKey();

	void setMapKeyForeignKey(JaxbForeignKeyImpl value);

	String getSqlRestriction();
	void setSqlRestriction(String sqlRestriction);

	JaxbCustomSqlImpl getSqlInsert();
	void setSqlInsert(JaxbCustomSqlImpl sqlInsert);

	JaxbCustomSqlImpl getSqlUpdate();
	void setSqlUpdate(JaxbCustomSqlImpl sqlUpdate);

	JaxbCustomSqlImpl getSqlDelete();
	void setSqlDelete(JaxbCustomSqlImpl sqlDelete);

	JaxbCustomSqlImpl getSqlDeleteAll();
	void setSqlDeleteAll(JaxbCustomSqlImpl sqlDeleteAll);

	List<JaxbFilterImpl> getFilters();

	@Override
	default Boolean isOptional() {
		return null;
	}

	@Override
	default void setOptional(Boolean optional) {

	}
}
