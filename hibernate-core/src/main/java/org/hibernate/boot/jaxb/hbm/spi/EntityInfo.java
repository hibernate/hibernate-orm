/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

/**
 * Common interface for all entity mappings (root entity and sub-entity mappings).
 *
 * @author Steve Ebersole
 */
public interface EntityInfo extends ToolingHintContainer {
	public String getName();
	public String getEntityName();

    public Boolean isAbstract();
    public Boolean isLazy();
    public String getProxy();
    public int getBatchSize();
    public boolean isDynamicInsert();
    public boolean isDynamicUpdate();
    public boolean isSelectBeforeUpdate();

	public List<JaxbHbmTuplizerType> getTuplizer();
    public String getPersister();

	public JaxbHbmLoaderType getLoader();
	public JaxbHbmCustomSqlDmlType getSqlInsert();
	public JaxbHbmCustomSqlDmlType getSqlUpdate();
	public JaxbHbmCustomSqlDmlType getSqlDelete();

	public List<JaxbHbmSynchronizeType> getSynchronize();

	public List<JaxbHbmFetchProfileType> getFetchProfile();

    public List<JaxbHbmResultSetMappingType> getResultset();

	public List<JaxbHbmNamedNativeQueryType> getSqlQuery();
	public List<JaxbHbmNamedQueryType> getQuery();

	public List getAttributes();
}
