/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

import org.hibernate.annotations.PolymorphismType;
import org.hibernate.engine.OptimisticLockStyle;

/**
 * @author Steve Ebersole
 */
public interface JaxbEntity extends JaxbEntityOrMappedSuperclass {
	String getName();
	void setName(String name);

	JaxbTableImpl getTable();
	void setTable(JaxbTableImpl value);

	String getTableExpression();
	void setTableExpression(String value);

	List<JaxbSecondaryTableImpl> getSecondaryTables();
	List<JaxbSynchronizedTableImpl> getSynchronizeTables();

	List<JaxbPrimaryKeyJoinColumnImpl> getPrimaryKeyJoinColumns();

	JaxbForeignKeyImpl getPrimaryKeyForeignKey();
	void setPrimaryKeyForeignKey(JaxbForeignKeyImpl value);

	String getRowid();
	void setRowid(String value);

	String getSqlRestriction();
	void setSqlRestriction(String value);

	JaxbSqlSelectImpl getSqlSelect();
	String getHqlSelect();

	JaxbCustomSqlImpl getSqlInsert();
	void setSqlInsert(JaxbCustomSqlImpl value);

	JaxbCustomSqlImpl getSqlUpdate();
	void setSqlUpdate(JaxbCustomSqlImpl value);

	JaxbCustomSqlImpl getSqlDelete();
	void setSqlDelete(JaxbCustomSqlImpl value);

	Boolean isDynamicInsert();
	void setDynamicInsert(Boolean value);

	Boolean isDynamicUpdate();
	void setDynamicUpdate(Boolean value);

	Boolean isSelectBeforeUpdate();
	void setSelectBeforeUpdate(Boolean value);

	JaxbCachingImpl getCaching();
	void setCaching(JaxbCachingImpl value);

	Integer getBatchSize();
	void setBatchSize(Integer value);

	Boolean isLazy();
	void setLazy(Boolean value);

	Boolean isMutable();
	void setMutable(Boolean value);

	OptimisticLockStyle getOptimisticLocking();
	void setOptimisticLocking(OptimisticLockStyle value);

	JaxbInheritanceImpl getInheritance();
	void setInheritance(JaxbInheritanceImpl value);

	String getProxy();
	void setProxy(String value);

	PolymorphismType getPolymorphism();
	void setPolymorphism(PolymorphismType value);

	String getDiscriminatorValue();
	void setDiscriminatorValue(String value);

	JaxbDiscriminatorColumnImpl getDiscriminatorColumn();
	void setDiscriminatorColumn(JaxbDiscriminatorColumnImpl value);

	String getDiscriminatorFormula();
	void setDiscriminatorFormula(String value);

	JaxbSequenceGeneratorImpl getSequenceGenerators();

	JaxbTableGeneratorImpl getTableGenerators();

	List<JaxbGenericIdGeneratorImpl> getIdentifierGenerators();

	List<JaxbNamedQueryImpl> getNamedQueries();
	List<JaxbNamedNativeQueryImpl> getNamedNativeQueries();
	List<JaxbNamedStoredProcedureQueryImpl> getNamedStoredProcedureQueries();
	List<JaxbSqlResultSetMappingImpl> getSqlResultSetMappings();

	List<JaxbAttributeOverrideImpl> getAttributeOverrides();
	List<JaxbAssociationOverrideImpl> getAssociationOverrides();

	List<JaxbConvertImpl> getConverts();

	List<JaxbNamedEntityGraphImpl> getNamedEntityGraphs();

	List<JaxbHbmFilterImpl> getFilters();

	List<JaxbFetchProfileImpl> getFetchProfiles();

	JaxbTenantIdImpl getTenantId();
	void setTenantId(JaxbTenantIdImpl value);

	JaxbAttributesContainerImpl getAttributes();
	void setAttributes(JaxbAttributesContainerImpl value);

	Boolean isCacheable();
	void setCacheable(Boolean value);

}
