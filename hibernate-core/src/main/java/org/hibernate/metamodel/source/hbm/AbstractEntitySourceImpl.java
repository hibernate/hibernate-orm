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
package org.hibernate.metamodel.source.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.CustomSQL;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.Origin;
import org.hibernate.metamodel.source.binder.AttributeSource;
import org.hibernate.metamodel.source.binder.EntitySource;
import org.hibernate.metamodel.source.binder.MetaAttributeSource;
import org.hibernate.metamodel.source.binder.SubclassEntitySource;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.EntityElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLAnyElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLManyToManyElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLManyToOneElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLOneToManyElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLOneToOneElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLPropertyElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSynchronizeElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLTuplizerElement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractEntitySourceImpl implements EntitySource {
	private final MappingDocument sourceMappingDocument;
	private final EntityElement entityElement;

	private List<SubclassEntitySource> subclassEntitySources = new ArrayList<SubclassEntitySource>();

	protected AbstractEntitySourceImpl(MappingDocument sourceMappingDocument, EntityElement entityElement) {
		this.sourceMappingDocument = sourceMappingDocument;
		this.entityElement = entityElement;
	}

	protected EntityElement entityElement() {
		return entityElement;
	}

	protected MappingDocument sourceMappingDocument() {
		return sourceMappingDocument;
	}

	@Override
	public Origin getOrigin() {
		return sourceMappingDocument.getOrigin();
	}

	@Override
	public LocalBindingContext getBindingContext() {
		return sourceMappingDocument.getMappingLocalBindingContext();
	}

	@Override
	public String getEntityName() {
		return StringHelper.isNotEmpty( entityElement.getEntityName() )
				? entityElement.getEntityName()
				: getClassName();
	}

	@Override
	public String getClassName() {
		return getBindingContext().qualifyClassName( entityElement.getName() );
	}

	@Override
	public String getJpaEntityName() {
		return null;
	}

	@Override
	public boolean isAbstract() {
		return Helper.getBooleanValue( entityElement.isAbstract(), false );
	}

	@Override
	public boolean isLazy() {
		return Helper.getBooleanValue( entityElement.isAbstract(), true );
	}

	@Override
	public String getProxy() {
		return entityElement.getProxy();
	}

	@Override
	public int getBatchSize() {
		return Helper.getIntValue( entityElement.getBatchSize(), -1 );
	}

	@Override
	public boolean isDynamicInsert() {
		return entityElement.isDynamicInsert();
	}

	@Override
	public boolean isDynamicUpdate() {
		return entityElement.isDynamicUpdate();
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return entityElement.isSelectBeforeUpdate();
	}

	protected EntityMode determineEntityMode() {
		return StringHelper.isNotEmpty( getClassName() ) ? EntityMode.POJO : EntityMode.MAP;
	}

	@Override
	public String getCustomTuplizerClassName() {
		if ( entityElement.getTuplizer() == null ) {
			return null;
		}
		final EntityMode entityMode = determineEntityMode();
		for ( XMLTuplizerElement tuplizerElement : entityElement.getTuplizer() ) {
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode() ) ) {
				return tuplizerElement.getClazz();
			}
		}
		return null;
	}

	@Override
	public String getCustomPersisterClassName() {
		return getBindingContext().qualifyClassName( entityElement.getPersister() );
	}

	@Override
	public String getCustomLoaderName() {
		return entityElement.getLoader() != null ? entityElement.getLoader().getQueryRef() : null;
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return Helper.buildCustomSql( entityElement.getSqlInsert() );
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return Helper.buildCustomSql( entityElement.getSqlUpdate() );
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return Helper.buildCustomSql( entityElement.getSqlDelete() );
	}

	@Override
	public List<String> getSynchronizedTableNames() {
		List<String> tableNames = new ArrayList<String>();
		for ( XMLSynchronizeElement synchronizeElement : entityElement.getSynchronize() ) {
			tableNames.add( synchronizeElement.getTable() );
		}
		return tableNames;
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Helper.buildMetaAttributeSources( entityElement.getMeta() );
	}

	@Override
	public Iterable<AttributeSource> attributeSources() {
		List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
		for ( Object attributeElement : entityElement.getPropertyOrManyToOneOrOneToOne() ) {
			if ( XMLPropertyElement.class.isInstance( attributeElement ) ) {
				attributeSources.add(
						new PropertyAttributeSourceImpl(
								XMLPropertyElement.class.cast( attributeElement ),
								sourceMappingDocument().getMappingLocalBindingContext()
						)
				);
			}
			else if ( XMLManyToOneElement.class.isInstance( attributeElement ) ) {
				attributeSources.add(
						new ManyToOneAttributeSourceImpl(
								XMLManyToOneElement.class.cast( attributeElement ),
								sourceMappingDocument().getMappingLocalBindingContext()
						)
				);
			}
			else if ( XMLOneToOneElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( XMLAnyElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( XMLOneToManyElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( XMLManyToManyElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
		}
		return attributeSources;
	}

	private EntityHierarchyImpl entityHierarchy;

	public void injectHierarchy(EntityHierarchyImpl entityHierarchy) {
		this.entityHierarchy = entityHierarchy;
	}

	@Override
	public void add(SubclassEntitySource subclassEntitySource) {
		add( (SubclassEntitySourceImpl) subclassEntitySource );
	}

	public void add(SubclassEntitySourceImpl subclassEntitySource) {
		entityHierarchy.processSubclass( subclassEntitySource );
		subclassEntitySources.add( subclassEntitySource );
	}

	@Override
	public Iterable<SubclassEntitySource> subclassEntitySources() {
		return subclassEntitySources;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
