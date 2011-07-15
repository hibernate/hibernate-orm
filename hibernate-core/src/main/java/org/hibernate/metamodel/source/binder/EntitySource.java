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
package org.hibernate.metamodel.source.binder;

import java.util.List;

import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.Origin;

/**
 * @author Steve Ebersole
 */
public interface EntitySource extends SubclassEntityContainer, AttributeSourceContainer {
	public Origin getOrigin();
	public LocalBindingContext getBindingContext();

	public String getEntityName();
	public String getClassName();
	public String getJpaEntityName();

	public TableSource getPrimaryTable();

    public boolean isAbstract();
    public boolean isLazy();
    public String getProxy();
    public int getBatchSize();
    public boolean isDynamicInsert();
    public boolean isDynamicUpdate();
    public boolean isSelectBeforeUpdate();

	public String getCustomTuplizerClassName();
    public String getCustomPersisterClassName();

	public String getCustomLoaderName();
	public CustomSQL getCustomSqlInsert();
	public CustomSQL getCustomSqlUpdate();
	public CustomSQL getCustomSqlDelete();

	public List<String> getSynchronizedTableNames();

	public Iterable<MetaAttributeSource> metaAttributes();

//	public List<XMLFetchProfileElement> getFetchProfile();

}
