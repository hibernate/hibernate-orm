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
 * @author Steve Ebersole
 */
public interface EntityElement extends MetaAttributeContainer {
	public String getName();
	public String getEntityName();

    public boolean isAbstract();
    public Boolean isLazy();
    public String getProxy();
    public int getBatchSize();
    public boolean isDynamicInsert();
    public boolean isDynamicUpdate();
    public boolean isSelectBeforeUpdate();

	public List<JaxbTuplizerElement> getTuplizer();
    public String getPersister();

	public JaxbLoaderElement getLoader();
	public JaxbSqlDmlElement getSqlInsert();
	public JaxbSqlDmlElement getSqlUpdate();
	public JaxbSqlDmlElement getSqlDelete();

	public List<JaxbSynchronizeElement> getSynchronize();

	public List<JaxbFetchProfileElement> getFetchProfile();

    public List<JaxbResultsetElement> getResultset();

	public List<JaxbSqlQueryElement> getSqlQuery();
	public List<JaxbQueryElement> getQuery();

	public List<JaxbPropertyElement> getProperty();
	public List<JaxbPropertiesElement> getProperties();
	public List<JaxbManyToOneElement> getManyToOne();
	public List<JaxbOneToOneElement> getOneToOne();
	public List<JaxbComponentElement> getComponent();
	public List<JaxbDynamicComponentElement> getDynamicComponent();
	public List<JaxbAnyElement> getAny();
	public List<JaxbMapElement> getMap();
	public List<JaxbSetElement> getSet();
	public List<JaxbListElement> getList();
	public List<JaxbBagElement> getBag();
	public List<JaxbIdbagElement> getIdbag();
	public List<JaxbArrayElement> getArray();
	public List<JaxbPrimitiveArrayElement> getPrimitiveArray();
}
