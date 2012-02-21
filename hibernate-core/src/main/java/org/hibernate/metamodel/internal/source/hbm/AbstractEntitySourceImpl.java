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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.internal.jaxb.Origin;
import org.hibernate.internal.jaxb.mapping.hbm.EntityElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbAnyElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbBagElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbComponentElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbIdbagElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbJoinElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbListElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbManyToOneElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbMapElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbOneToOneElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbPropertyElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbSetElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbSynchronizeElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbTuplizerElement;
import org.hibernate.internal.jaxb.mapping.hbm.JoinElementSource;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.JpaCallbackSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;

/**
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public abstract class AbstractEntitySourceImpl implements EntitySource {
	private final MappingDocument sourceMappingDocument;
	private final EntityElement entityElement;
	private final Set<SecondaryTableSource> secondaryTableSources;

	private List<SubclassEntitySource> subclassEntitySources = new ArrayList<SubclassEntitySource>();

	protected AbstractEntitySourceImpl(MappingDocument sourceMappingDocument, EntityElement entityElement) {
		this.sourceMappingDocument = sourceMappingDocument;
		this.entityElement = entityElement;

		secondaryTableSources = extractSecondaryTables( entityElement, sourceMappingDocument.getMappingLocalBindingContext() );
	}

	private static Set<SecondaryTableSource> extractSecondaryTables(EntityElement entityElement, HbmBindingContext bindingContext) {
		if ( ! JoinElementSource.class.isInstance( entityElement ) ) {
			return Collections.emptySet();
		}

		final Set<SecondaryTableSource> secondaryTableSources = new HashSet<SecondaryTableSource>();
		for ( JaxbJoinElement joinElement :  ( (JoinElementSource) entityElement ).getJoin() ) {
			secondaryTableSources.add( new SecondaryTableSourceImpl( joinElement, bindingContext ) );
		}
		return secondaryTableSources;
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
	public LocalBindingContext getLocalBindingContext() {
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
		return getLocalBindingContext().qualifyClassName( entityElement.getName() );
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
		for ( JaxbTuplizerElement tuplizerElement : entityElement.getTuplizer() ) {
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode() ) ) {
				return tuplizerElement.getClazz();
			}
		}
		return null;
	}

	@Override
	public String getCustomPersisterClassName() {
		return getLocalBindingContext().qualifyClassName( entityElement.getPersister() );
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
		for ( JaxbSynchronizeElement synchronizeElement : entityElement.getSynchronize() ) {
			tableNames.add( synchronizeElement.getTable() );
		}
		return tableNames;
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Helper.buildMetaAttributeSources( entityElement.getMeta() );
	}

	@Override
	public String getPath() {
		return sourceMappingDocument.getMappingLocalBindingContext().determineEntityName( entityElement );
	}

	@Override
	public List<AttributeSource> attributeSources() {
		List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
		processAttributes( attributeSources );
		return attributeSources;
	}

	protected List<AttributeSource> processAttributes(List<AttributeSource> attributeSources) {
		processAttributes(
				attributeSources,
				entityElement.getPropertyOrManyToOneOrOneToOne(),
				SingularAttributeSource.NaturalIdMutability.NOT_NATURAL_ID
		);
		return attributeSources;
	}

	protected void processAttributes(
			List<AttributeSource> results,
			List attributeElements,
			SingularAttributeSource.NaturalIdMutability naturalIdMutability) {
		for ( Object attributeElement : attributeElements ) {
			if ( JaxbPropertyElement.class.isInstance( attributeElement ) ) {
				results.add(
						new PropertyAttributeSourceImpl(
								JaxbPropertyElement.class.cast( attributeElement ),
								sourceMappingDocument().getMappingLocalBindingContext(),
								naturalIdMutability
						)
				);
			}
			else if ( JaxbComponentElement.class.isInstance( attributeElement ) ) {
				results.add(
						new ComponentAttributeSourceImpl(
								(JaxbComponentElement) attributeElement,
								this,
								sourceMappingDocument.getMappingLocalBindingContext(),
								naturalIdMutability
						)
				);
			}
			else if ( JaxbManyToOneElement.class.isInstance( attributeElement ) ) {
				results.add(
						new ManyToOneAttributeSourceImpl(
								JaxbManyToOneElement.class.cast( attributeElement ),
								sourceMappingDocument().getMappingLocalBindingContext(),
								naturalIdMutability
						)
				);
			}
			else if ( JaxbOneToOneElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( JaxbAnyElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( JaxbBagElement.class.isInstance( attributeElement ) ) {
				results.add(
						new BagAttributeSourceImpl(
								JaxbBagElement.class.cast( attributeElement ),
								this
						)
				);
			}
			else if ( JaxbIdbagElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( JaxbSetElement.class.isInstance( attributeElement ) ) {
				results.add(
						new SetAttributeSourceImpl(
								JaxbSetElement.class.cast( attributeElement ),
								this
						)
				);
			}
			else if ( JaxbListElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else if ( JaxbMapElement.class.isInstance( attributeElement ) ) {
				// todo : implement
			}
			else {
				throw new AssertionFailure( "Unexpected attribute element type encountered : " + attributeElement.getClass() );
			}
		}
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

	@Override
	public Iterable<ConstraintSource> getConstraints() {
		return Collections.emptySet();
	}

	@Override
	public Set<SecondaryTableSource> getSecondaryTables() {
		return secondaryTableSources;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public List<JpaCallbackSource> getJpaCallbackClasses() {
	    return Collections.EMPTY_LIST;
	}
}
