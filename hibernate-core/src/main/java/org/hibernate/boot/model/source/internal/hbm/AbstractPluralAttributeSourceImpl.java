/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.AssertionFailure;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmColumnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToOneType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSynchronizeType;
import org.hibernate.boot.jaxb.hbm.spi.PluralAttributeInfo;
import org.hibernate.boot.model.source.spi.Caching;
import org.hibernate.boot.model.CustomSql;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.CollectionIdSource;
import org.hibernate.boot.model.source.spi.FetchCharacteristicsPluralAttribute;
import org.hibernate.boot.model.source.spi.FilterSource;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSource;
import org.hibernate.boot.model.source.spi.PluralAttributeKeySource;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;
import org.hibernate.boot.model.source.spi.TableSpecificationSource;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class AbstractPluralAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeSource, Helper.InLineViewNameInferrer {

	private static final FilterSource[] NO_FILTER_SOURCES = new FilterSource[0];

	private final PluralAttributeInfo pluralAttributeJaxbMapping;
	private final AttributeSourceContainer container;

	private final AttributeRole attributeRole;
	private final AttributePath attributePath;

	private final HibernateTypeSource typeInformation;

	private final PluralAttributeKeySource keySource;
	private final PluralAttributeElementSource elementSource;

	private final FetchCharacteristicsPluralAttributeImpl fetchCharacteristics;

	private final Caching caching;
	private final FilterSource[] filterSources;
	private final String[] synchronizedTableNames;
	private final ToolingHintContext toolingHintContext;

	protected AbstractPluralAttributeSourceImpl(
			MappingDocument mappingDocument,
			final PluralAttributeInfo pluralAttributeJaxbMapping,
			AttributeSourceContainer container) {
		super( mappingDocument );
		this.pluralAttributeJaxbMapping = pluralAttributeJaxbMapping;
		this.container = container;

		this.attributeRole = container.getAttributeRoleBase().append( pluralAttributeJaxbMapping.getName() );
		this.attributePath = container.getAttributePathBase().append( pluralAttributeJaxbMapping.getName() );

		Optional<JaxbHbmManyToOneType> jaxbHbmManyToOneTypeOptional = Optional.empty();

		// Our goal here is to find the inverse side of a one to many to figure out against what to join
		if ( pluralAttributeJaxbMapping.isInverse() && pluralAttributeJaxbMapping.getOneToMany() != null && pluralAttributeJaxbMapping.getKey().getPropertyRef() == null ) {
			String childClass = pluralAttributeJaxbMapping.getOneToMany().getClazz();

			if ( childClass != null ) {
				// We match by columns as defined in the key
				final List<String> keyColumnNames;
				if ( pluralAttributeJaxbMapping.getKey().getColumnAttribute() == null ) {
					keyColumnNames = new ArrayList<>( pluralAttributeJaxbMapping.getKey().getColumn().size() );
					for ( JaxbHbmColumnType jaxbHbmColumnType : pluralAttributeJaxbMapping.getKey().getColumn() ) {
						keyColumnNames.add( jaxbHbmColumnType.getName() );
					}
				}
				else {
					keyColumnNames = new ArrayList<>( 1 );
					keyColumnNames.add( pluralAttributeJaxbMapping.getKey().getColumnAttribute() );
				}
				jaxbHbmManyToOneTypeOptional = mappingDocument.getDocumentRoot().getClazz()
						.stream()
						.filter( (JaxbHbmRootEntityType entityType) -> childClass.equals( entityType.getName() ) )
						.flatMap( jaxbHbmRootEntityType -> jaxbHbmRootEntityType.getAttributes().stream() )
						.filter( attribute -> {
							if ( attribute instanceof JaxbHbmManyToOneType manyToOneType ) {
								String manyToOneTypeClass = manyToOneType.getClazz();
								String containerClass = container.getAttributeRoleBase().getFullPath();
								// Consider many to ones that have no class defined or equal the owner class of the one to many
								if ( manyToOneTypeClass == null || manyToOneTypeClass.equals( containerClass ) ) {
									if ( manyToOneType.getColumnAttribute() == null ) {
										List<Serializable> columns = manyToOneType.getColumnOrFormula();
										if ( columns.size() != keyColumnNames.size() ) {
											return false;
										}
										for ( int i = 0; i < columns.size(); i++ ) {
											Serializable column = columns.get( i );
											String keyColumn = keyColumnNames.get( i );
											if ( !( column instanceof JaxbHbmColumnType ) || !( (JaxbHbmColumnType) column )
													.getName()
													.equals( keyColumn ) ) {
												return false;
											}
										}
									}
									else {
										return keyColumnNames.size() == 1 && keyColumnNames.get( 0 )
												.equals( manyToOneType.getColumnAttribute() );
									}
									return true;
								}
							}
							return false;
						})
						.map( JaxbHbmManyToOneType.class::cast )
						.findFirst();
			}
		}

		this.keySource = jaxbHbmManyToOneTypeOptional
				.map( jaxbHbmManyToOneType -> new PluralAttributeKeySourceImpl(
						sourceMappingDocument(),
						pluralAttributeJaxbMapping.getKey(),
						jaxbHbmManyToOneType,
						container
				) ).orElseGet( () -> new PluralAttributeKeySourceImpl(
						sourceMappingDocument(),
						pluralAttributeJaxbMapping.getKey(),
						container
				) );

		this.typeInformation = new HibernateTypeSourceImpl( pluralAttributeJaxbMapping.getCollectionType() );

		this.caching = Helper.createCaching( pluralAttributeJaxbMapping.getCache() );

		this.filterSources = buildFilterSources( mappingDocument, pluralAttributeJaxbMapping );
		this.synchronizedTableNames = extractSynchronizedTableNames( pluralAttributeJaxbMapping );
		this.toolingHintContext = Helper.collectToolingHints(
				container.getToolingHintContext(),
				pluralAttributeJaxbMapping
		);

		this.elementSource = interpretElementType();

		this.fetchCharacteristics = FetchCharacteristicsPluralAttributeImpl.interpret(
				mappingDocument.getEffectiveDefaults(),
				pluralAttributeJaxbMapping.getFetch(),
				pluralAttributeJaxbMapping.getOuterJoin(),
				pluralAttributeJaxbMapping.getLazy(),
				pluralAttributeJaxbMapping.getBatchSize()
		);
	}

	private static String[] extractSynchronizedTableNames(PluralAttributeInfo pluralAttributeElement) {
		if ( pluralAttributeElement.getSynchronize().isEmpty() ) {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}

		final String[] names = new String[pluralAttributeElement.getSynchronize().size()];
		int i = 0;
		for ( JaxbHbmSynchronizeType jaxbHbmSynchronizeType : pluralAttributeElement.getSynchronize() ) {
			names[i++] = jaxbHbmSynchronizeType.getTable();
		}

		return names;
	}

	private static FilterSource[] buildFilterSources(
			MappingDocument mappingDocument,
			PluralAttributeInfo pluralAttributeElement) {
		final int size = pluralAttributeElement.getFilter().size();
		if ( size == 0 ) {
			return null;
		}

		FilterSource[] results = new FilterSource[size];
		for ( int i = 0; i < size; i++ ) {
			JaxbHbmFilterType element = pluralAttributeElement.getFilter().get( i );
			results[i] = new FilterSourceImpl( mappingDocument, element );
		}
		return results;

	}

	private PluralAttributeElementSource interpretElementType() {
		if ( pluralAttributeJaxbMapping.getElement() != null ) {
			return new PluralAttributeElementSourceBasicImpl(
					sourceMappingDocument(),
					this,
					pluralAttributeJaxbMapping.getElement()
			);
		}
		else if ( pluralAttributeJaxbMapping.getCompositeElement() != null ) {
			return new PluralAttributeElementSourceEmbeddedImpl(
					sourceMappingDocument(),
					this,
					pluralAttributeJaxbMapping.getCompositeElement()
			);
		}
		else if ( pluralAttributeJaxbMapping.getOneToMany() != null ) {
			return new PluralAttributeElementSourceOneToManyImpl(
					sourceMappingDocument(),
					this,
					pluralAttributeJaxbMapping.getOneToMany(),
					pluralAttributeJaxbMapping.getCascade()
			);
		}
		else if ( pluralAttributeJaxbMapping.getManyToMany() != null ) {
			return new PluralAttributeElementSourceManyToManyImpl(
					sourceMappingDocument(),
					this,
					pluralAttributeJaxbMapping.getManyToMany()
			);
		}
		else if ( pluralAttributeJaxbMapping.getManyToAny() != null ) {
			return new PluralAttributeElementSourceManyToAnyImpl(
					sourceMappingDocument(),
					this,
					pluralAttributeJaxbMapping.getManyToAny(),
					pluralAttributeJaxbMapping.getCascade()
			);
		}
		else {
			throw new MappingException(
					"Unexpected collection element type : " + pluralAttributeJaxbMapping.getName(),
					sourceMappingDocument().getOrigin()
			);
		}
	}

	@Override
	public AttributePath getAttributePath() {
		return attributePath;
	}

	@Override
	public AttributeRole getAttributeRole() {
		return attributeRole;
	}

	@Override
	public boolean usesJoinTable() {
		switch ( elementSource.getNature() ) {
			case BASIC:
			case AGGREGATE:
			case ONE_TO_MANY:
				return false;
			case MANY_TO_MANY:
				return true;
			case MANY_TO_ANY:
				throw new AssertionFailure(
						String.format( "%s is not implemented yet.", elementSource.getNature() )
				);
			default:
				throw new AssertionFailure(
						String.format(
								"Unexpected plural attribute element source nature: %s",
								elementSource.getNature()
						)
				);
		}
	}

	protected AttributeSourceContainer container() {
		return container;
	}

	@Override
	public FilterSource[] getFilterSources() {
		return filterSources == null
				? NO_FILTER_SOURCES
				: filterSources;
	}

	@Override
	public PluralAttributeKeySource getKeySource() {
		return keySource;
	}

	@Override
	public PluralAttributeElementSource getElementSource() {
		return elementSource;
	}

	@Override
	public String getCascadeStyleName() {
		return pluralAttributeJaxbMapping.getCascade();
	}

	@Override
	public boolean isMutable() {
		return pluralAttributeJaxbMapping.isMutable();
	}

	@Override
	public String getMappedBy() {
		return null;
	}

	@Override
	public String inferInLineViewName() {
		return getAttributeRole().getFullPath();
	}

	@Override
	public CollectionIdSource getCollectionIdSource() {
		return null;
	}

	@Override
	public TableSpecificationSource getCollectionTableSpecificationSource() {
		return pluralAttributeJaxbMapping.getOneToMany() == null
				? Helper.createTableSource( sourceMappingDocument(), pluralAttributeJaxbMapping, this )
				: null;
	}

	@Override
	public String getCollectionTableComment() {
		return pluralAttributeJaxbMapping.getComment();
	}

	@Override
	public String getCollectionTableCheck() {
		return pluralAttributeJaxbMapping.getCheck();
	}

	@Override
	public String[] getSynchronizedTableNames() {
		return synchronizedTableNames;
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public String getWhere() {
		return pluralAttributeJaxbMapping.getWhere();
	}

	@Override
	public String getName() {
		return pluralAttributeJaxbMapping.getName();
	}

	@Override
	public boolean isSingular() {
		return false;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return typeInformation;
	}

	@Override
	public String getPropertyAccessorName() {
		return pluralAttributeJaxbMapping.getAccess();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return pluralAttributeJaxbMapping.isOptimisticLock();
	}

	@Override
	public boolean isInverse() {
		return pluralAttributeJaxbMapping.isInverse();
	}

	@Override
	public String getCustomPersisterClassName() {
		return pluralAttributeJaxbMapping.getPersister();
	}

	@Override
	public String getCustomLoaderName() {
		return pluralAttributeJaxbMapping.getLoader() == null
				? null
				: pluralAttributeJaxbMapping.getLoader().getQueryRef();
	}

	@Override
	public CustomSql getCustomSqlInsert() {
		return Helper.buildCustomSql( pluralAttributeJaxbMapping.getSqlInsert() );
	}

	@Override
	public CustomSql getCustomSqlUpdate() {
		return Helper.buildCustomSql( pluralAttributeJaxbMapping.getSqlUpdate() );
	}

	@Override
	public CustomSql getCustomSqlDelete() {
		return Helper.buildCustomSql( pluralAttributeJaxbMapping.getSqlDelete() );
	}

	@Override
	public CustomSql getCustomSqlDeleteAll() {
		return Helper.buildCustomSql( pluralAttributeJaxbMapping.getSqlDeleteAll() );
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}

	@Override
	public FetchCharacteristicsPluralAttribute getFetchCharacteristics() {
		return fetchCharacteristics;
	}
}
