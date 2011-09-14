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
package org.hibernate.metamodel.source.annotations.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.internal.jaxb.Origin;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.source.annotations.attribute.SingularAttributeSourceImpl;
import org.hibernate.metamodel.source.annotations.attribute.ToOneAttributeSourceImpl;
import org.hibernate.metamodel.source.binder.AttributeSource;
import org.hibernate.metamodel.source.binder.ConstraintSource;
import org.hibernate.metamodel.source.binder.EntitySource;
import org.hibernate.metamodel.source.binder.JpaCallbackClass;
import org.hibernate.metamodel.source.binder.MetaAttributeSource;
import org.hibernate.metamodel.source.binder.SubclassEntitySource;
import org.hibernate.metamodel.source.binder.TableSource;

/**
 * @author Hardy Ferentschik
 */
public class EntitySourceImpl implements EntitySource {
	private final EntityClass entityClass;
	private final Set<SubclassEntitySource> subclassEntitySources;

	public EntitySourceImpl(EntityClass entityClass) {
		this.entityClass = entityClass;
		this.subclassEntitySources = new HashSet<SubclassEntitySource>();
	}

	public EntityClass getEntityClass() {
		return entityClass;
	}

	@Override
	public Origin getOrigin() {
		return entityClass.getLocalBindingContext().getOrigin();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return entityClass.getLocalBindingContext();
	}

	@Override
	public String getEntityName() {
		return entityClass.getName();
	}

	@Override
	public String getClassName() {
		return entityClass.getName();
	}

	@Override
	public String getJpaEntityName() {
		return entityClass.getExplicitEntityName();
	}

	@Override
	public TableSource getPrimaryTable() {
		return entityClass.getPrimaryTableSource();
	}

	@Override
	public boolean isAbstract() {
		return false;
	}

	@Override
	public boolean isLazy() {
		return entityClass.isLazy();
	}

	@Override
	public String getProxy() {
		return entityClass.getProxy();
	}

	@Override
	public int getBatchSize() {
		return entityClass.getBatchSize();
	}

	@Override
	public boolean isDynamicInsert() {
		return entityClass.isDynamicInsert();
	}

	@Override
	public boolean isDynamicUpdate() {
		return entityClass.isDynamicUpdate();
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return entityClass.isSelectBeforeUpdate();
	}

	@Override
	public String getCustomTuplizerClassName() {
		return entityClass.getCustomTuplizer();
	}

	@Override
	public String getCustomPersisterClassName() {
		return entityClass.getCustomPersister();
	}

	@Override
	public String getCustomLoaderName() {
		return entityClass.getCustomLoaderQueryName();
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return entityClass.getCustomInsert();
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return entityClass.getCustomUpdate();
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return entityClass.getCustomDelete();
	}

	@Override
	public List<String> getSynchronizedTableNames() {
		return entityClass.getSynchronizedTableNames();
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Collections.emptySet();
	}

	@Override
	public String getPath() {
		return entityClass.getName();
	}

	@Override
	public Iterable<AttributeSource> attributeSources() {
		List<AttributeSource> attributeList = new ArrayList<AttributeSource>();
		for ( BasicAttribute attribute : entityClass.getSimpleAttributes() ) {
			attributeList.add( new SingularAttributeSourceImpl( attribute ) );
		}
		for ( EmbeddableClass component : entityClass.getEmbeddedClasses().values() ) {
			attributeList.add(
					new ComponentAttributeSourceImpl(
							component,
							"",
							entityClass.getAttributeOverrideMap()
					)
			);
		}
		for ( AssociationAttribute associationAttribute : entityClass.getAssociationAttributes() ) {
			attributeList.add( new ToOneAttributeSourceImpl( associationAttribute ) );
		}
		return attributeList;
	}

	@Override
	public void add(SubclassEntitySource subclassEntitySource) {
		subclassEntitySources.add( subclassEntitySource );
	}

	@Override
	public Iterable<SubclassEntitySource> subclassEntitySources() {
		return subclassEntitySources;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return entityClass.getDiscriminatorMatchValue();
	}

	@Override
	public Iterable<ConstraintSource> getConstraints() {
		return entityClass.getConstraintSources();
	}

	@Override
	public List<JpaCallbackClass> getJpaCallbackClasses() {
		return entityClass.getJpaCallbacks();
	}

	@Override
	public Iterable<TableSource> getSecondaryTables() {
		return entityClass.getSecondaryTableSources();
	}
}


