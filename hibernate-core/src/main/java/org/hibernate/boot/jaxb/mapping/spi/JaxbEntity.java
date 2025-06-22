/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import java.util.List;

import org.hibernate.engine.OptimisticLockStyle;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Steve Ebersole
 */
public interface JaxbEntity extends JaxbEntityOrMappedSuperclass {
	@Nullable String getName();
	void setName(@Nullable String name);

	@Nullable JaxbTableImpl getTable();
	void setTable(@Nullable JaxbTableImpl value);

	@Nullable String getTableExpression();
	void setTableExpression(@Nullable String value);

	List<JaxbSecondaryTableImpl> getSecondaryTables();
	List<JaxbSynchronizedTableImpl> getSynchronizeTables();

	List<JaxbPrimaryKeyJoinColumnImpl> getPrimaryKeyJoinColumns();

	@Nullable JaxbForeignKeyImpl getPrimaryKeyForeignKey();
	void setPrimaryKeyForeignKey(@Nullable JaxbForeignKeyImpl value);

	@Nullable String getRowid();
	void setRowid(@Nullable String value);

	@Nullable String getSqlRestriction();
	void setSqlRestriction(@Nullable String value);

	@Nullable JaxbSqlSelectImpl getSqlSelect();
	@Nullable String getHqlSelect();

	@Nullable JaxbCustomSqlImpl getSqlInsert();
	void setSqlInsert(JaxbCustomSqlImpl value);

	@Nullable JaxbCustomSqlImpl getSqlUpdate();
	void setSqlUpdate(@Nullable JaxbCustomSqlImpl value);

	@Nullable JaxbCustomSqlImpl getSqlDelete();
	void setSqlDelete(@Nullable JaxbCustomSqlImpl value);

	@Nullable Boolean isDynamicInsert();
	void setDynamicInsert(@Nullable Boolean value);

	@Nullable Boolean isDynamicUpdate();
	void setDynamicUpdate(@Nullable Boolean value);

	@Nullable Boolean isSelectBeforeUpdate();
	void setSelectBeforeUpdate(@Nullable Boolean value);

	@Nullable JaxbCachingImpl getCaching();
	void setCaching(@Nullable JaxbCachingImpl value);

	@Nullable Integer getBatchSize();
	void setBatchSize(@Nullable Integer value);

	@Nullable Boolean isLazy();
	void setLazy(@Nullable Boolean value);

	@Nullable Boolean isMutable();
	void setMutable(@Nullable Boolean value);

	@Nullable OptimisticLockStyle getOptimisticLocking();
	void setOptimisticLocking(@Nullable OptimisticLockStyle value);

	@Nullable JaxbInheritanceImpl getInheritance();
	void setInheritance(@Nullable JaxbInheritanceImpl value);

	@Nullable String getProxy();
	void setProxy(@Nullable String value);

	@Nullable String getDiscriminatorValue();
	void setDiscriminatorValue(@Nullable String value);

	@Nullable JaxbDiscriminatorColumnImpl getDiscriminatorColumn();
	void setDiscriminatorColumn(@Nullable JaxbDiscriminatorColumnImpl value);

	@Nullable JaxbDiscriminatorFormulaImpl getDiscriminatorFormula();
	void setDiscriminatorFormula(@Nullable JaxbDiscriminatorFormulaImpl value);

	@Nullable JaxbSequenceGeneratorImpl getSequenceGenerator();

	@Nullable JaxbTableGeneratorImpl getTableGenerator();

	@Nullable JaxbGenericIdGeneratorImpl getGenericGenerator();

	List<JaxbNamedHqlQueryImpl> getNamedQueries();
	List<JaxbNamedNativeQueryImpl> getNamedNativeQueries();
	List<JaxbNamedStoredProcedureQueryImpl> getNamedStoredProcedureQueries();
	List<JaxbSqlResultSetMappingImpl> getSqlResultSetMappings();

	List<JaxbAttributeOverrideImpl> getAttributeOverrides();
	List<JaxbAssociationOverrideImpl> getAssociationOverrides();

	List<JaxbConvertImpl> getConverts();

	List<JaxbNamedEntityGraphImpl> getNamedEntityGraphs();

	List<JaxbFilterImpl> getFilters();

	List<JaxbFetchProfileImpl> getFetchProfiles();

	@Nullable JaxbTenantIdImpl getTenantId();
	void setTenantId(@Nullable JaxbTenantIdImpl value);

	@Nullable JaxbAttributesContainerImpl getAttributes();
	void setAttributes(@Nullable JaxbAttributesContainerImpl value);

	@Nullable Boolean isCacheable();
	void setCacheable(@Nullable Boolean value);

}
