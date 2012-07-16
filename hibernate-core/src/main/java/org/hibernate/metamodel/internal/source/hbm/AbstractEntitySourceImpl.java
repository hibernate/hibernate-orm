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
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.JpaCallbackSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;

/**
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public abstract class AbstractEntitySourceImpl
		extends AbstractHbmSourceNode
		implements EntitySource, Helper.InLineViewNameInferrer {

	private final EntityElement entityElement;
	private final String className;
	private final String entityName;

	private List<SubclassEntitySource> subclassEntitySources = new ArrayList<SubclassEntitySource>();

	private int inLineViewCount = 0;

	// logically final, but built during 'afterInstantiation' callback
	private List<AttributeSource> attributeSources;
	private Set<SecondaryTableSource> secondaryTableSources;

	protected AbstractEntitySourceImpl(MappingDocument sourceMappingDocument, EntityElement entityElement) {
		super( sourceMappingDocument );
		this.entityElement = entityElement;

		this.className = bindingContext().qualifyClassName( entityElement.getName() );
		this.entityName = StringHelper.isNotEmpty( entityElement.getEntityName() )
				? entityElement.getEntityName()
				: className;
	}

	@Override
	public String inferInLineViewName() {
		return entityName + '#' + (++inLineViewCount);
	}

	protected void afterInstantiation() {
		this.attributeSources = buildAttributeSources();
		this.secondaryTableSources = buildSecondaryTables();
	}

	protected List<AttributeSource> buildAttributeSources() {
		List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
		buildAttributeSources( attributeSources );
		return attributeSources;
	}

	protected List<AttributeSource> buildAttributeSources(List<AttributeSource> attributeSources) {
		processAttributes(
				attributeSources,
				entityElement.getPropertyOrManyToOneOrOneToOne(),
				null,
				SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID
		);
		return attributeSources;
	}

	protected void processAttributes(
			List<AttributeSource> results,
			List attributeElements,
			String logicalTableName,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		for ( Object attributeElement : attributeElements ) {
			results.add( buildAttributeSource( attributeElement, logicalTableName, naturalIdMutability ) );
		}
	}

	protected AttributeSource buildAttributeSource(
			Object attributeElement,
			String logicalTableName,
			SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		if ( JaxbPropertyElement.class.isInstance( attributeElement ) ) {
			return new PropertyAttributeSourceImpl(
					sourceMappingDocument(),
					JaxbPropertyElement.class.cast( attributeElement ),
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbComponentElement.class.isInstance( attributeElement ) ) {
			return new ComponentAttributeSourceImpl(
					sourceMappingDocument(),
					(JaxbComponentElement) attributeElement,
					this,
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbManyToOneElement.class.isInstance( attributeElement ) ) {
			return new ManyToOneAttributeSourceImpl(
					sourceMappingDocument(),
					JaxbManyToOneElement.class.cast( attributeElement ),
					logicalTableName,
					naturalIdMutability
			);
		}
		else if ( JaxbOneToOneElement.class.isInstance( attributeElement ) ) {
			// todo : implement
		}
		else if ( JaxbAnyElement.class.isInstance( attributeElement ) ) {
			// todo : implement
		}
		else if ( JaxbBagElement.class.isInstance( attributeElement ) ) {
			return new BagAttributeSourceImpl(
					sourceMappingDocument(),
					JaxbBagElement.class.cast( attributeElement ),
					this
			);
		}
		else if ( JaxbIdbagElement.class.isInstance( attributeElement ) ) {
			// todo : implement
		}
		else if ( JaxbSetElement.class.isInstance( attributeElement ) ) {
			return new SetAttributeSourceImpl(
					sourceMappingDocument(),
					JaxbSetElement.class.cast( attributeElement ),
					this
			);
		}
		else if ( JaxbListElement.class.isInstance( attributeElement ) ) {
			return new ListAttributeSource(
					sourceMappingDocument(),
					JaxbListElement.class.cast( attributeElement ),
					this
			);
		}
		else if ( JaxbMapElement.class.isInstance( attributeElement ) ) {
			return new MapAttributeSource(
					sourceMappingDocument(),
					JaxbMapElement.class.cast( attributeElement ),
					this
			);
		}

		throw new UnexpectedAttributeSourceTypeException(
				"Unexpected attribute element type encountered : " + attributeElement.getClass().getName()
		);
	}

	private Set<SecondaryTableSource> buildSecondaryTables() {
		if ( ! JoinElementSource.class.isInstance( entityElement ) ) {
			return Collections.emptySet();
		}

		final Set<SecondaryTableSource> secondaryTableSources = new HashSet<SecondaryTableSource>();
		for ( JaxbJoinElement joinElement :  ( (JoinElementSource) entityElement ).getJoin() ) {
			final SecondaryTableSourceImpl secondaryTableSource = new SecondaryTableSourceImpl(
					sourceMappingDocument(),
					joinElement,
					this
			);
			secondaryTableSources.add( secondaryTableSource );

			final String logicalTableName = secondaryTableSource.getLogicalTableNameForContainedColumns();
			processAttributes(
					attributeSources,
					joinElement.getPropertyOrManyToOneOrComponent(),
					logicalTableName,
					SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID
			);
		}
		return secondaryTableSources;
	}

	protected EntityElement entityElement() {
		return entityElement;
	}

	@Override
	public Origin getOrigin() {
		return origin();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return bindingContext();
	}

	@Override
	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getJpaEntityName() {
		return null;
	}

	@Override
	public boolean isAbstract() {
		return entityElement().isAbstract();
	}

	@Override
	public boolean isLazy() {
		return entityElement().isLazy();
	}

	@Override
	public String getProxy() {
		return entityElement.getProxy();
	}

	@Override
	public int getBatchSize() {
		return entityElement.getBatchSize();
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
			if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode().value() ) ) {
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
		return bindingContext().determineEntityName( entityElement );
	}

	@Override
	public List<AttributeSource> attributeSources() {
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
		return null;
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
