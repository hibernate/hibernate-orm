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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.hbm.EntityElement;
import org.hibernate.jaxb.spi.hbm.JaxbAnyElement;
import org.hibernate.jaxb.spi.hbm.JaxbArrayElement;
import org.hibernate.jaxb.spi.hbm.JaxbBagElement;
import org.hibernate.jaxb.spi.hbm.JaxbClassElement;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbComponentElement;
import org.hibernate.jaxb.spi.hbm.JaxbDynamicComponentElement;
import org.hibernate.jaxb.spi.hbm.JaxbFilterElement;
import org.hibernate.jaxb.spi.hbm.JaxbIdbagElement;
import org.hibernate.jaxb.spi.hbm.JaxbJoinElement;
import org.hibernate.jaxb.spi.hbm.JaxbListElement;
import org.hibernate.jaxb.spi.hbm.JaxbManyToOneElement;
import org.hibernate.jaxb.spi.hbm.JaxbMapElement;
import org.hibernate.jaxb.spi.hbm.JaxbOneToOneElement;
import org.hibernate.jaxb.spi.hbm.JaxbPropertyElement;
import org.hibernate.jaxb.spi.hbm.JaxbSetElement;
import org.hibernate.jaxb.spi.hbm.JaxbTuplizerElement;
import org.hibernate.jaxb.spi.hbm.JoinElementSource;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.ConstraintSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.FilterSource;
import org.hibernate.metamodel.spi.source.JpaCallbackSource;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;

/**
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public abstract class AbstractEntitySourceImpl
		extends AbstractHbmSourceNode
		implements EntitySource, Helper.InLineViewNameInferrer {

	private final EntityElement entityElement;
	private final String className;
	private final String entityName;
	private final String jpaEntityName;

	private List<SubclassEntitySource> subclassEntitySources = new ArrayList<SubclassEntitySource>();

	private int inLineViewCount = 0;

	// logically final, but built during 'afterInstantiation' callback
	private List<AttributeSource> attributeSources;
	private Set<SecondaryTableSource> secondaryTableSources;
	private final FilterSource[] filterSources;
	
	private Map<String, ConstraintSource> constraintMap = new HashMap<String, ConstraintSource>();
	
	protected AbstractEntitySourceImpl(MappingDocument sourceMappingDocument, EntityElement entityElement) {
		super( sourceMappingDocument );
		this.entityElement = entityElement;

		this.className = bindingContext().qualifyClassName( entityElement.getName() );
		if ( StringHelper.isNotEmpty( entityElement.getEntityName() ) ) {
			this.entityName = entityElement.getEntityName();
			this.jpaEntityName = entityElement.getEntityName();
		}
		else {
			this.entityName = className;
			this.jpaEntityName = StringHelper.unqualify( className );
		}
		this.filterSources = buildFilterSources();
	}

	private FilterSource[] buildFilterSources() {
		//todo for now, i think all EntityElement should support this.
		if ( JaxbClassElement.class.isInstance( entityElement() ) ) {
			JaxbClassElement jaxbClassElement = JaxbClassElement.class.cast( entityElement() );
			final int size = jaxbClassElement.getFilter().size();
			if ( size == 0 ) {
				return null;
			}

			FilterSource[] results = new FilterSource[size];
			for ( int i = 0; i < size; i++ ) {
				JaxbFilterElement element = jaxbClassElement.getFilter().get( i );
				results[i] = new FilterSourceImpl( sourceMappingDocument(), element );
			}
			return results;
		}
		else {
			return null;
		}

	}

	@Override
	public FilterSource[] getFilterSources() {
		return filterSources;
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
		return buildAttributeSources( entityElement, attributeSources, null, SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID );
	}
	protected List<AttributeSource> buildAttributeSources(EntityElement element,
														  List<AttributeSource> attributeSources,
														  String logicTalbeName,
														  SingularAttributeBinding.NaturalIdMutability naturalIdMutability){
		processPropertyAttributes( attributeSources, element.getProperty(), logicTalbeName, naturalIdMutability );
		processComponentAttributes(
				attributeSources,
				element.getComponent(),
				logicTalbeName,
				naturalIdMutability
		);
		processDynamicComponentAttributes(
				attributeSources,
				element.getDynamicComponent(),
				logicTalbeName,
				naturalIdMutability
		);
		processManyToOneAttributes(
				attributeSources,
				element.getManyToOne(),
				logicTalbeName,
				naturalIdMutability
		);
		processOneToOneAttributes(
				attributeSources,
				element.getOneToOne(),
				logicTalbeName,
				naturalIdMutability
		);
		processAnyAttributes(
				attributeSources,
				element.getAny(),
				logicTalbeName,
				naturalIdMutability
		);
		processMapAttributes( attributeSources, element.getMap() );
		processListAttributes( attributeSources, element.getList() );
		processArrayAttributes( attributeSources, element.getArray() );
		processSetAttributes( attributeSources, element.getSet() );
		processIdBagAttributes( attributeSources, element.getIdbag() );
		processBagAttributes( attributeSources, element.getBag() );
		return attributeSources;
	}

	protected void processPropertyAttributes(List<AttributeSource> results,
											 List<JaxbPropertyElement> propertyElements,
											 String logicalTableName,
											 SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		for ( JaxbPropertyElement element : propertyElements ) {
			results.add(
					new PropertyAttributeSourceImpl(
							sourceMappingDocument(),
							element,
							logicalTableName,
							naturalIdMutability
					)
			);
			
			// TODO: Not sure this is the right place to do this.  Can index constraints be defined by anything other
			// than just a property?  Split off into its own process method?
			for ( JaxbColumnElement column : element.getColumn() ) {
				// An index constraint can happen on the column element or the surrounding property element.
				if ( !StringHelper.isEmpty( column.getIndex() ) ) {
					addColumnToIndexConstraint( column.getIndex(), logicalTableName, column.getName() );
				}
				if ( !StringHelper.isEmpty( element.getIndex() ) ) {
					addColumnToIndexConstraint( element.getIndex(), logicalTableName, column.getName() );
				}
			}
		}
	}
	
	private void addColumnToIndexConstraint(String constraintName, String logicalTableName, String columnName) {
		if ( !constraintMap.containsKey( constraintName ) ) {
			constraintMap.put( constraintName, new IndexConstraintSourceImpl( constraintName, logicalTableName ) );
		}
		( (AbstractConstraintSource) constraintMap.get( constraintName ) ).addColumnName( columnName );
	}

	protected void processComponentAttributes(List<AttributeSource> results,
											 List<JaxbComponentElement> elements,
											 String logicalTableName,
											 SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		for ( JaxbComponentElement element : elements ) {
			results.add(
					new ComponentAttributeSourceImpl(
							sourceMappingDocument(),
							element,
							this,
							logicalTableName,
							naturalIdMutability
					)
			);
		}
	}

	protected void processDynamicComponentAttributes(List<AttributeSource> results,
											  List<JaxbDynamicComponentElement> elements,
											  String logicalTableName,
											  SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		// todo : implement
	}

	protected void processManyToOneAttributes(List<AttributeSource> results,
											  List<JaxbManyToOneElement> elements,
											  String logicalTableName,
											  SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		for ( JaxbManyToOneElement element : elements ) {
			results.add(
					new ManyToOneAttributeSourceImpl(
							sourceMappingDocument(),
							element,
							logicalTableName,
							naturalIdMutability
					)
			);
		}
	}
	protected void processOneToOneAttributes(List<AttributeSource> results,
											   List<JaxbOneToOneElement> elements,
											   String logicalTableName,
											   SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		for ( JaxbOneToOneElement element : elements ) {
			results.add(
					new OneToOneAttributeSourceImpl(
							sourceMappingDocument(),
							element,
							logicalTableName,
							naturalIdMutability
					)
			);
		}
	}

	protected void processAnyAttributes(List<AttributeSource> results,
											  List<JaxbAnyElement> elements,
											  String logicalTableName,
											  SingularAttributeBinding.NaturalIdMutability naturalIdMutability) {
		// todo : implement
	}

	protected void processMapAttributes(List<AttributeSource> results,
											 List<JaxbMapElement> propertyElements){
		for ( JaxbMapElement element : propertyElements ) {
			results.add(
					new MapSourceImpl(
							sourceMappingDocument(),
							element, this
					)
			);
		}
	}
	protected void processArrayAttributes(List<AttributeSource> results,
			List<JaxbArrayElement> propertyElements){
		for ( JaxbArrayElement element : propertyElements ) {
			results.add(
					new ArraySourceImpl(
							sourceMappingDocument(),
							element, this
							)
					);
		}
	}
	protected void processListAttributes(List<AttributeSource> results,
											 List<JaxbListElement> propertyElements){
		for ( JaxbListElement element : propertyElements ) {
			results.add(
					new ListSourceImpl(
							sourceMappingDocument(),
							element, this
					)
			);
		}
	}
	protected void processSetAttributes(List<AttributeSource> results,
											 List<JaxbSetElement> propertyElements){
		for ( JaxbSetElement element : propertyElements ) {
			results.add(
					new SetSourceImpl(
							sourceMappingDocument(),
							element,
							this
					)
			);
		}
	}
	protected void processIdBagAttributes(List<AttributeSource> results,
											 List<JaxbIdbagElement> propertyElements){

		if ( !propertyElements.isEmpty() ) {
			throw new NotYetImplementedException( "<idbag> is not supported yet" );
		}
	}
	protected void processBagAttributes(List<AttributeSource> results,
											 List<JaxbBagElement> propertyElements) {
		for ( JaxbBagElement element : propertyElements ) {
			results.add(
					new BagSourceImpl(
							sourceMappingDocument(),
							element,
							this
					)
			);
		}
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
			final SingularAttributeBinding.NaturalIdMutability  naturalIdMutability = SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID;
			processAnyAttributes(
					attributeSources,
					joinElement.getAny(),
					logicalTableName,
					naturalIdMutability
			);
			processComponentAttributes(
					attributeSources,
					joinElement.getComponent(),
					logicalTableName,
					naturalIdMutability
			);
			processDynamicComponentAttributes(
					attributeSources,
					joinElement.getDynamicComponent(),
					logicalTableName,
					naturalIdMutability
			);
			processManyToOneAttributes(
					attributeSources,
					joinElement.getManyToOne(),
					logicalTableName,
					naturalIdMutability
			);
			processPropertyAttributes(
					attributeSources,
					joinElement.getProperty(),
					logicalTableName,
					naturalIdMutability
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
		return jpaEntityName;
	}

	@Override
	public boolean isAbstract() {
		return entityElement().isAbstract();
	}

	@Override
	public boolean isLazy() {
		if(entityElement.isLazy()==null){
			return getLocalBindingContext().getMappingDefaults().areAssociationsLazy();
		}
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
	public String[] getSynchronizedTableNames() {
		if ( CollectionHelper.isEmpty( entityElement.getSynchronize() ) ) {
			return StringHelper.EMPTY_STRINGS;
		}
		else {
			final int size = entityElement.getSynchronize().size();
			final String[] synchronizedTableNames = new String[size];
			for ( int i = 0; i < size; i++ ) {
				synchronizedTableNames[i] = entityElement.getSynchronize().get( i ).getTable();
			}
			return synchronizedTableNames;
		}
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		return entityElement.getMeta();
	}

	@Override
	public String getPath() {
		return "";
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
		subclassEntitySource.injectHierarchy( entityHierarchy );
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
		return constraintMap.values();
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
