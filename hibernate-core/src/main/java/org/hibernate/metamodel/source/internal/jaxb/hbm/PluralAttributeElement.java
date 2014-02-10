/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.jaxb.hbm;

import java.util.List;

/**
 * Commonality between the various forms of plural attribute (collection) mappings: {@code <bag/>}, {@code <set/>}, etc.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElement extends TableInformationSource, MetaAttributeContainer {
	public String getName();
	public String getAccess();

	public JaxbKeyElement getKey();

	public JaxbElementElement getElement();
	public JaxbCompositeElementElement getCompositeElement();
	public JaxbOneToManyElement getOneToMany();
	public JaxbManyToManyElement getManyToMany();
    public JaxbManyToAnyElement getManyToAny();

	public String getComment();
	public String getCheck();
	public String getWhere();

	public JaxbLoaderElement getLoader();
	public JaxbSqlDmlElement getSqlInsert();
    public JaxbSqlDmlElement getSqlUpdate();
    public JaxbSqlDmlElement getSqlDelete();
    public JaxbSqlDmlElement getSqlDeleteAll();

	public List<JaxbSynchronizeElement> getSynchronize();

	public JaxbCacheElement getCache();
	public List<JaxbFilterElement> getFilter();

	public String getCascade();
	public JaxbFetchAttributeWithSubselect getFetch();
	public JaxbLazyAttributeWithExtra getLazy();
	public JaxbOuterJoinAttribute getOuterJoin();

	public int getBatchSize();
	public boolean isInverse();
    public boolean isMutable();
	public boolean isOptimisticLock();

	public String getCollectionType();
    public String getPersister();

// todo : not available on all.  do we need a specific interface for these?
//	public String getSort();
//	public String getOrderBy();
}
