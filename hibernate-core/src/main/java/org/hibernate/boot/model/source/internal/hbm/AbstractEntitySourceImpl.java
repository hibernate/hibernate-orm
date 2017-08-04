/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.hbm.spi.EntityInfo;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityBaseDefinition;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchProfileType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;
import org.hibernate.boot.jaxb.hbm.spi.SecondaryTableContainer;
import org.hibernate.boot.model.CustomSql;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.EntityHierarchySource;
import org.hibernate.boot.model.source.spi.EntityNamingSource;
import org.hibernate.boot.model.source.spi.EntitySource;
import org.hibernate.boot.model.source.spi.FilterSource;
import org.hibernate.boot.model.source.spi.IdentifiableTypeSource;
import org.hibernate.boot.model.source.spi.JpaCallbackSource;
import org.hibernate.boot.model.source.spi.LocalMetadataBuildingContext;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.SecondaryTableSource;
import org.hibernate.boot.model.source.spi.SubclassEntitySource;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public abstract class AbstractEntitySourceImpl
		extends AbstractHbmSourceNode
		implements EntitySource, Helper.InLineViewNameInferrer {

	private static final FilterSource[] NO_FILTER_SOURCES = new FilterSource[0];

	private final JaxbHbmEntityBaseDefinition jaxbEntityMapping;
	private final EntityNamingSource entityNamingSource;

	private final AttributeRole attributeRoleBase;
	private final AttributePath attributePathBase;

	private List<IdentifiableTypeSource> subclassEntitySources = new ArrayList<IdentifiableTypeSource>();

	private int inLineViewCount = 0;

	// logically final, but built during 'afterInstantiation' callback
	private List<AttributeSource> attributeSources;
	private Map<String,SecondaryTableSource> secondaryTableMap;
	private final FilterSource[] filterSources;

	private final Map<EntityMode, String> tuplizerClassMap;

	private final ToolingHintContext toolingHintContext;

	protected AbstractEntitySourceImpl(MappingDocument sourceMappingDocument, JaxbHbmEntityBaseDefinition jaxbEntityMapping) {
		super( sourceMappingDocument );
		this.jaxbEntityMapping = jaxbEntityMapping;

		this.entityNamingSource = extractEntityNamingSource( sourceMappingDocument, jaxbEntityMapping );

		this.attributePathBase = new AttributePath();
		this.attributeRoleBase = new AttributeRole( entityNamingSource.getEntityName() );

		this.tuplizerClassMap = extractTuplizers( jaxbEntityMapping );

		this.filterSources = buildFilterSources();

		for ( JaxbHbmFetchProfileType jaxbFetchProfile : jaxbEntityMapping.getFetchProfile() ) {
			FetchProfileBinder.processFetchProfile(
					sourceMappingDocument,
					jaxbFetchProfile,
					entityNamingSource.getClassName() != null
							? entityNamingSource.getClassName()
							: entityNamingSource.getEntityName()
			);
		}

		this.toolingHintContext = Helper.collectToolingHints(
				sourceMappingDocument.getToolingHintContext(),
				jaxbEntityMapping
		);
	}

	public static EntityNamingSourceImpl extractEntityNamingSource(
			MappingDocument sourceMappingDocument,
			EntityInfo jaxbEntityMapping) {
		final String className = sourceMappingDocument.qualifyClassName( jaxbEntityMapping.getName() );
		final String entityName;
		final String jpaEntityName;
		if ( StringHelper.isNotEmpty( jaxbEntityMapping.getEntityName() ) ) {
			entityName = jaxbEntityMapping.getEntityName();
			jpaEntityName = jaxbEntityMapping.getEntityName();
		}
		else {
			entityName = className;
			jpaEntityName = StringHelper.unqualify( className );
		}
		return new EntityNamingSourceImpl( entityName, className, jpaEntityName );
	}

	private static Map<EntityMode, String> extractTuplizers(JaxbHbmEntityBaseDefinition entityElement) {
		if ( entityElement.getTuplizer() == null ) {
			return Collections.emptyMap();
		}

		final Map<EntityMode, String> tuplizers = new HashMap<EntityMode, String>();
		for ( JaxbHbmTuplizerType tuplizerElement : entityElement.getTuplizer() ) {
			tuplizers.put(
					tuplizerElement.getEntityMode(),
					tuplizerElement.getClazz()
			);
		}
		return tuplizers;
	}

	private FilterSource[] buildFilterSources() {
		//todo for now, i think all EntityElement should support this.
		if ( JaxbHbmRootEntityType.class.isInstance( jaxbEntityMapping() ) ) {
			final JaxbHbmRootEntityType jaxbClassElement = (JaxbHbmRootEntityType) jaxbEntityMapping();
			final int size = jaxbClassElement.getFilter().size();
			if ( size == 0 ) {
				return NO_FILTER_SOURCES;
			}

			FilterSource[] results = new FilterSource[size];
			for ( int i = 0; i < size; i++ ) {
				JaxbHbmFilterType element = jaxbClassElement.getFilter().get( i );
				results[i] = new FilterSourceImpl( sourceMappingDocument(), element );
			}
			return results;
		}
		else {
			return NO_FILTER_SOURCES;
		}

	}

	@Override
	public String getXmlNodeName() {
		return jaxbEntityMapping.getNode();
	}

	@Override
	public LocalMetadataBuildingContext getLocalMetadataBuildingContext() {
		return super.metadataBuildingContext();
	}

	@Override
	public String getTypeName() {
		return entityNamingSource.getTypeName();
	}

	@Override
	public AttributePath getAttributePathBase() {
		return attributePathBase;
	}

	@Override
	public AttributeRole getAttributeRoleBase() {
		return attributeRoleBase;
	}

	@Override
	public Collection<IdentifiableTypeSource> getSubTypes() {
		return subclassEntitySources;
	}

	@Override
	public FilterSource[] getFilterSources() {
		return filterSources;
	}

	@Override
	public String inferInLineViewName() {
		return entityNamingSource.getEntityName() + '#' + (++inLineViewCount);
	}

	protected void afterInstantiation() {
		this.attributeSources = buildAttributeSources();
		this.secondaryTableMap = buildSecondaryTableMap();
	}

	protected List<AttributeSource> buildAttributeSources() {
		final List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();

		AttributesHelper.Callback attributeBuildingCallback = new AttributesHelper.Callback() {
			@Override
			public AttributeSourceContainer getAttributeSourceContainer() {
				return AbstractEntitySourceImpl.this;
			}

			@Override
			public void addAttributeSource(AttributeSource attributeSource) {
				attributeSources.add( attributeSource );
			}
		};
		buildAttributeSources( attributeBuildingCallback );

		return attributeSources;
	}

	protected void buildAttributeSources(AttributesHelper.Callback attributeBuildingCallback) {
		AttributesHelper.processAttributes(
				sourceMappingDocument(),
				attributeBuildingCallback,
				jaxbEntityMapping.getAttributes(),
				null,
				NaturalIdMutability.NOT_NATURAL_ID
		);
	}

	private Map<String,SecondaryTableSource> buildSecondaryTableMap() {
		if ( !SecondaryTableContainer.class.isInstance( jaxbEntityMapping ) ) {
			return Collections.emptyMap();
		}

		final HashMap<String,SecondaryTableSource> secondaryTableSourcesMap =
				new HashMap<String, SecondaryTableSource>();

		for ( final JaxbHbmSecondaryTableType joinElement :  ( (SecondaryTableContainer) jaxbEntityMapping ).getJoin() ) {
			final SecondaryTableSourceImpl secondaryTableSource = new SecondaryTableSourceImpl(
					sourceMappingDocument(),
					joinElement,
					getEntityNamingSource(),
					this
			);

			final String logicalTableName = secondaryTableSource.getLogicalTableNameForContainedColumns();
			secondaryTableSourcesMap.put( logicalTableName, secondaryTableSource );

			AttributesHelper.processAttributes(
					sourceMappingDocument(),
					new AttributesHelper.Callback() {
						@Override
						public AttributeSourceContainer getAttributeSourceContainer() {
							return AbstractEntitySourceImpl.this;
						}

						@Override
						public void addAttributeSource(AttributeSource attributeSource) {
							attributeSources.add( attributeSource );
						}
					},
					joinElement.getAttributes(),
					logicalTableName,
					NaturalIdMutability.NOT_NATURAL_ID
			);
		}
		return secondaryTableSourcesMap;
	}

	protected JaxbHbmEntityBaseDefinition jaxbEntityMapping() {
		return jaxbEntityMapping;
	}

	@Override
	public Origin getOrigin() {
		return origin();
	}

	@Override
	public EntityNamingSource getEntityNamingSource() {
		return entityNamingSource;
	}

	@Override
	public Boolean isAbstract() {
		return jaxbEntityMapping().isAbstract();
	}

	@Override
	public boolean isLazy() {
		if ( jaxbEntityMapping.isLazy() == null ) {
			return metadataBuildingContext().getMappingDefaults().areEntitiesImplicitlyLazy();
		}
		return jaxbEntityMapping().isLazy();
	}

	@Override
	public String getProxy() {
		return jaxbEntityMapping.getProxy();
	}

	@Override
	public int getBatchSize() {
		return jaxbEntityMapping.getBatchSize();
	}

	@Override
	public boolean isDynamicInsert() {
		return jaxbEntityMapping.isDynamicInsert();
	}

	@Override
	public boolean isDynamicUpdate() {
		return jaxbEntityMapping.isDynamicUpdate();
	}

	@Override
	public boolean isSelectBeforeUpdate() {
		return jaxbEntityMapping.isSelectBeforeUpdate();
	}

	protected EntityMode determineEntityMode() {
		return StringHelper.isNotEmpty( entityNamingSource.getClassName() ) ? EntityMode.POJO : EntityMode.MAP;
	}

	@Override
	public Map<EntityMode, String> getTuplizerClassMap() {
		return tuplizerClassMap;
	}

	@Override
	public String getCustomPersisterClassName() {
		return metadataBuildingContext().qualifyClassName( jaxbEntityMapping.getPersister() );
	}

	@Override
	public String getCustomLoaderName() {
		return jaxbEntityMapping.getLoader() != null ? jaxbEntityMapping.getLoader().getQueryRef() : null;
	}

	@Override
	public CustomSql getCustomSqlInsert() {
		return Helper.buildCustomSql( jaxbEntityMapping.getSqlInsert() );
	}

	@Override
	public CustomSql getCustomSqlUpdate() {
		return Helper.buildCustomSql( jaxbEntityMapping.getSqlUpdate() );
	}

	@Override
	public CustomSql getCustomSqlDelete() {
		return Helper.buildCustomSql( jaxbEntityMapping.getSqlDelete() );
	}

	@Override
	public String[] getSynchronizedTableNames() {
		if ( CollectionHelper.isEmpty( jaxbEntityMapping.getSynchronize() ) ) {
			return StringHelper.EMPTY_STRINGS;
		}
		else {
			final int size = jaxbEntityMapping.getSynchronize().size();
			final String[] synchronizedTableNames = new String[size];
			for ( int i = 0; i < size; i++ ) {
				synchronizedTableNames[i] = jaxbEntityMapping.getSynchronize().get( i ).getTable();
			}
			return synchronizedTableNames;
		}
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}

	@Override
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	private EntityHierarchySourceImpl entityHierarchy;

	public void injectHierarchy(EntityHierarchySourceImpl entityHierarchy) {
		this.entityHierarchy = entityHierarchy;
	}

	@Override
	public EntityHierarchySource getHierarchy() {
		return entityHierarchy;
	}

	void add(SubclassEntitySource subclassEntitySource) {
		add( (SubclassEntitySourceImpl) subclassEntitySource );
	}

	void add(SubclassEntitySourceImpl subclassEntitySource) {
		subclassEntitySource.injectHierarchy( entityHierarchy );
		entityHierarchy.processSubclass( subclassEntitySource );
		subclassEntitySources.add( subclassEntitySource );
	}

	@Override
	public Map<String,SecondaryTableSource> getSecondaryTableMap() {
		return secondaryTableMap;
	}

	@Override
	public List<JpaCallbackSource> getJpaCallbackClasses() {
		return Collections.emptyList();
	}

	@Override
	public List<JaxbHbmNamedQueryType> getNamedQueries() {
		return jaxbEntityMapping.getQuery();
	}

	@Override
	public List<JaxbHbmNamedNativeQueryType> getNamedNativeQueries() {
		return jaxbEntityMapping.getSqlQuery();
	}

	@Override
	public TruthValue quoteIdentifiersLocalToEntity() {
		// HBM does not allow for this
		return TruthValue.UNKNOWN;
	}

}
