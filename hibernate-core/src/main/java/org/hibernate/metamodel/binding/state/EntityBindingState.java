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
package org.hibernate.metamodel.binding.state;

import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.domain.Hierarchical;
import org.hibernate.metamodel.source.spi.MetaAttributeContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.tuple.entity.EntityTuplizer;

/**
 * Represents unified set of information about metadata specific to binding an entity.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public interface EntityBindingState {
	/**
	 * Obtain the Hibernate entity name.
	 *
	 * @return The entity name.
	 */
	public String getEntityName();

	/**
	 * Obtain the JPA entity name.
	 *
	 * @return  The JPA entity name
	 */
	public String getJpaEntityName();

	/**
	 * Obtain the entity mode represented by this state.
	 *
	 * @return The entity mode.
	 */
	public EntityMode getEntityMode();

	/**
	 * Obtain the name of the entity class.
	 *
	 * @return The entity class name.
	 */
	public String getClassName();

	/**
	 * The name of an interface to use for creating instance proxies for this entity.
	 *
	 * @return The name of the proxy interface.
	 */
	public String getProxyInterfaceName();

	public Class<EntityPersister> getCustomEntityPersisterClass();

	public Class<EntityTuplizer> getCustomEntityTuplizerClass();

	public Hierarchical getSuperType();

	boolean isRoot();

	InheritanceType getEntityInheritanceType();

	Caching getCaching();

	MetaAttributeContext getMetaAttributeContext();

	boolean isLazy();

	boolean isMutable();

	boolean isExplicitPolymorphism();

	String getWhereFilter();

	String getRowId();

	boolean isDynamicUpdate();

	boolean isDynamicInsert();

	int getBatchSize();

	boolean isSelectBeforeUpdate();

	int getOptimisticLockMode();


	Boolean isAbstract();

	CustomSQL getCustomInsert();

	CustomSQL getCustomUpdate();

	CustomSQL getCustomDelete();

	Set<String> getSynchronizedTableNames();
}
