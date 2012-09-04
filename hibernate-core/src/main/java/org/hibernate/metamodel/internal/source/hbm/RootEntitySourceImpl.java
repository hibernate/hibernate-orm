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
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.TruthValue;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jaxb.spi.hbm.JaxbClassElement;
import org.hibernate.jaxb.spi.hbm.JaxbCompositeIdElement;
import org.hibernate.jaxb.spi.hbm.JaxbDiscriminatorElement;
import org.hibernate.jaxb.spi.hbm.JaxbKeyManyToOneElement;
import org.hibernate.jaxb.spi.hbm.JaxbKeyPropertyElement;
import org.hibernate.jaxb.spi.hbm.JaxbMultiTenancyElement;
import org.hibernate.jaxb.spi.hbm.JaxbNaturalIdElement;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.IdGenerator;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.source.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.AttributeSource;
import org.hibernate.metamodel.spi.source.ComponentAttributeSource;
import org.hibernate.metamodel.spi.source.DiscriminatorSource;
import org.hibernate.metamodel.spi.source.IdentifierSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.MultiTenancySource;
import org.hibernate.metamodel.spi.source.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.RootEntitySource;
import org.hibernate.metamodel.spi.source.SimpleIdentifierSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;
import org.hibernate.metamodel.spi.source.VersionAttributeSource;

/**
 * @author Steve Ebersole
 */
public class RootEntitySourceImpl extends AbstractEntitySourceImpl implements RootEntitySource {
	private final TableSpecificationSource primaryTable;
	private final Caching caching;
	private final ValueHolder<Caching> naturalIdCachingHolder;

	protected RootEntitySourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbClassElement entityElement) {
		super( sourceMappingDocument, entityElement );
		this.primaryTable = Helper.createTableSource( sourceMappingDocument(), entityElement, this );
		this.caching = Helper.createCaching( entityElement().getCache(), getEntityName() );
		this.naturalIdCachingHolder = Helper.createNaturalIdCachingHolder(
				entityElement.getNaturalIdCache(),
				getEntityName(),
				caching
		);
		afterInstantiation();
	}

	@Override
	protected JaxbClassElement entityElement() {
		return (JaxbClassElement) super.entityElement();
	}

	@Override
	public IdentifierSource getIdentifierSource() {
		if ( entityElement().getId() == null && entityElement().getCompositeId() == null ) {
			throw new AssertionFailure(
					String.format( "Entity [%s] did not define an identifier", getEntityName() )
			);
		}

		if ( entityElement().getId() != null ) {
			return new SimpleIdentifierSourceImpl();
		}
		else {
			// if we get here, we should have a composite identifier.  Just need to determine if it is aggregated, or not...
			if ( StringHelper.isEmpty( entityElement().getCompositeId().getName() ) ) {
				if ( entityElement().getCompositeId().isMapped() &&
						StringHelper.isEmpty( entityElement().getCompositeId().getClazz() ) ) {
					throw makeMappingException( "mapped composite identifier must name component class to use." );
				}
				return new NonAggregatedCompositeIdentifierSourceImpl();
			}
			else {
				if ( entityElement().getCompositeId().isMapped() ) {
					throw makeMappingException("cannot combine mapped=\"true\" with specified name");
				}
				return new AggregatedCompositeIdentifierSourceImpl();
			}
		}
	}

	@Override
	public VersionAttributeSource getVersioningAttributeSource() {
		if ( entityElement().getVersion() != null ) {
			return new VersionAttributeSourceImpl(
					sourceMappingDocument(),
					entityElement().getVersion()
			);
		}
		else if ( entityElement().getTimestamp() != null ) {
			return new TimestampAttributeSourceImpl(
					sourceMappingDocument(),
					entityElement().getTimestamp()
			);
		}
		return null;
	}

	@Override
	protected List<AttributeSource> buildAttributeSources(List<AttributeSource> attributeSources) {
		final JaxbNaturalIdElement naturalId = entityElement().getNaturalId();
		if ( naturalId != null ) {
			final SingularAttributeBinding.NaturalIdMutability naturalIdMutability = naturalId.isMutable() ? SingularAttributeBinding.NaturalIdMutability.MUTABLE : SingularAttributeBinding.NaturalIdMutability.IMMUTABLE;
			processPropertyAttributes( attributeSources, naturalId.getProperty(), null, naturalIdMutability );
			processManyToOneAttributes( attributeSources, naturalId.getManyToOne(), null, naturalIdMutability );
			processComponentAttributes( attributeSources, naturalId.getComponent(), null, naturalIdMutability );
			processDynamicComponentAttributes( attributeSources, naturalId.getDynamicComponent(), null, naturalIdMutability );
			processAnyAttributes( attributeSources, naturalId.getAny(), null, naturalIdMutability );
		}
		return super.buildAttributeSources( attributeSources );
	}

	@Override
	public EntityMode getEntityMode() {
		return determineEntityMode();
	}

	@Override
	public boolean isMutable() {
		return entityElement().isMutable();
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return "explicit".equals( entityElement().getPolymorphism() );
	}

	@Override
	public String getWhere() {
		return entityElement().getWhere();
	}

	@Override
	public String getRowId() {
		return entityElement().getRowid();
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		final String optimisticLockModeString = Helper.getValue( entityElement().getOptimisticLock().value(), "version" );
		try {
			return OptimisticLockStyle.valueOf( optimisticLockModeString.toUpperCase() );
		}
		catch ( Exception e ) {
			throw new MappingException(
					"Unknown optimistic-lock value : " + optimisticLockModeString,
					sourceMappingDocument().getOrigin()
			);
		}
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public Caching getNaturalIdCaching() {
		return naturalIdCachingHolder.getValue();
	}

	@Override
	public TableSpecificationSource getPrimaryTable() {
		return primaryTable;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return entityElement().getDiscriminatorValue();
	}

	@Override
	public DiscriminatorSource getDiscriminatorSource() {
		final JaxbDiscriminatorElement discriminatorElement = entityElement().getDiscriminator();
		if ( discriminatorElement == null ) {
			return null;
		}

		return new DiscriminatorSource() {
			@Override
			public RelationalValueSource getDiscriminatorRelationalValueSource() {
				if ( StringHelper.isNotEmpty( discriminatorElement.getColumnAttribute() ) ) {
					return new ColumnAttributeSourceImpl(
							sourceMappingDocument(),
							null, // root table
							discriminatorElement.getColumnAttribute(),
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE,
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE
					);
				}
				else if ( StringHelper.isNotEmpty( discriminatorElement.getFormulaAttribute() ) ) {
					return new FormulaImpl(
							sourceMappingDocument(),
							null,
							discriminatorElement.getFormulaAttribute()
					);
				}
				else if ( discriminatorElement.getColumn() != null ) {
					return new ColumnSourceImpl(
							sourceMappingDocument(),
							null, // root table
							discriminatorElement.getColumn(),
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE,
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE
					);
				}
				else if ( StringHelper.isNotEmpty( discriminatorElement.getFormula() ) ) {
					return new FormulaImpl(
							sourceMappingDocument(),
							null,
							discriminatorElement.getFormula()
					);
				}
				else {
					throw new MappingException( "could not determine source of discriminator mapping", getOrigin() );
				}
			}

			@Override
			public String getExplicitHibernateTypeName() {
				return discriminatorElement.getType();
			}

			@Override
			public boolean isForced() {
				return discriminatorElement.isForce();
			}

			@Override
			public boolean isInserted() {
				return discriminatorElement.isInsert();
			}
		};
	}

	@Override
	public MultiTenancySource getMultiTenancySource() {
		final JaxbMultiTenancyElement jaxbMultiTenancy = entityElement().getMultiTenancy();
		if ( jaxbMultiTenancy == null ) {
			return null;
		}

		return new MultiTenancySource() {
			@Override
			public RelationalValueSource getRelationalValueSource() {

				if ( StringHelper.isNotEmpty( jaxbMultiTenancy.getColumnAttribute() ) ) {
					return new ColumnAttributeSourceImpl(
							sourceMappingDocument(),
							null, // root table
							jaxbMultiTenancy.getColumnAttribute(),
							TruthValue.TRUE,
							TruthValue.FALSE
					);
				}
				else if ( StringHelper.isNotEmpty( jaxbMultiTenancy.getFormulaAttribute() ) ) {
					return new FormulaImpl(
							sourceMappingDocument(),
							null,
							jaxbMultiTenancy.getFormulaAttribute()
					);
				}
				else if ( jaxbMultiTenancy.getColumn() != null ) {
					return new ColumnSourceImpl(
							sourceMappingDocument(),
							null, // root table
							jaxbMultiTenancy.getColumn(),
							TruthValue.TRUE,
							TruthValue.FALSE
					);
				}
				else if ( StringHelper.isNotEmpty( jaxbMultiTenancy.getFormula() ) ) {
					return new FormulaImpl(
							sourceMappingDocument(),
							null,
							jaxbMultiTenancy.getFormula()
					);
				}
				else {
					return null;
				}
			}

			@Override
			public boolean isShared() {
				return jaxbMultiTenancy.isShared();
			}

			@Override
			public boolean bindAsParameter() {
				return jaxbMultiTenancy.isBindAsParam();
			}
		};
	}

	private class SimpleIdentifierSourceImpl implements SimpleIdentifierSource {
		@Override
		public SingularAttributeSource getIdentifierAttributeSource() {
			return new SingularIdentifierAttributeSourceImpl(
					sourceMappingDocument(),
					entityElement().getId()
			);
		}

		@Override
		public IdGenerator getIdentifierGeneratorDescriptor() {
			if ( entityElement().getId().getGenerator() != null ) {
				final String generatorName = entityElement().getId().getGenerator().getClazz();
				IdGenerator idGenerator = sourceMappingDocument().getMappingLocalBindingContext()
						.getMetadataImplementor()
						.getIdGenerator( generatorName );
				if ( idGenerator == null ) {
					idGenerator = new IdGenerator(
							getEntityName() + generatorName,
							generatorName,
							Helper.extractParameters( entityElement().getId().getGenerator().getParam() )
					);
				}
				return idGenerator;
			}
			return null;
		}

		@Override
		public EntityIdentifierNature getNature() {
			return EntityIdentifierNature.SIMPLE;
		}

		@Override
		public String getUnsavedValue() {
			return entityElement().getId().getUnsavedValue();
		}

		@Override
		public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
			return entityElement().getId().getMeta();
		}
	}

	private class AggregatedCompositeIdentifierSourceImpl implements AggregatedCompositeIdentifierSource {
		private final CompositeIdentifierComponentAttributeSourceImpl componentAttributeSource
				= new CompositeIdentifierComponentAttributeSourceImpl();

		@Override
		public ComponentAttributeSource getIdentifierAttributeSource() {
			return componentAttributeSource;
		}

		@Override
		public IdGenerator getIndividualAttributeIdGenerator(String identifierAttributeName) {
			// for now, return null.  this is that stupid specj bs
			return null;
		}

		@Override
		public IdGenerator getIdentifierGeneratorDescriptor() {
			if ( entityElement().getCompositeId().getGenerator() != null ) {
				final String generatorName = entityElement().getCompositeId().getGenerator().getClazz();
				IdGenerator idGenerator = sourceMappingDocument().getMappingLocalBindingContext()
						.getMetadataImplementor()
						.getIdGenerator( generatorName );
				if ( idGenerator == null ) {
					idGenerator = new IdGenerator(
							getEntityName() + generatorName,
							generatorName,
							Helper.extractParameters( entityElement().getCompositeId().getGenerator().getParam() )
					);
				}
				return idGenerator;
			}
			return null;
		}

		@Override
		public EntityIdentifierNature getNature() {
			return EntityIdentifierNature.AGGREGATED_COMPOSITE;
		}

		@Override
		public String getUnsavedValue() {
			return entityElement().getCompositeId().getUnsavedValue().value();
		}

		@Override
		public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
			return entityElement().getId().getMeta();
		}
	}

	private class CompositeIdentifierComponentAttributeSourceImpl extends AbstractComponentAttributeSourceImpl {
		protected CompositeIdentifierComponentAttributeSourceImpl() {
			super(
					RootEntitySourceImpl.this.sourceMappingDocument(),
					entityElement().getCompositeId(),
					RootEntitySourceImpl.this,
					null,
					SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID
			);
		}

		protected JaxbCompositeIdElement compositeIdElement() {
			return (JaxbCompositeIdElement) componentSourceElement();
		}

		@Override
		protected List<AttributeSource> buildAttributeSources() {
			List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();
//			for ( Object attributeElement : compositeIdElement().getKeyPropertyOrKeyManyToOne() ) {
//				attributeSources.add( buildAttributeSource( attributeElement ) );
//			}
			for ( JaxbKeyPropertyElement keyProperty : compositeIdElement().getKeyProperty()){
				attributeSources.add( new IdentifierKeyAttributeSourceImpl( sourceMappingDocument(), keyProperty ) );
			}
			for (JaxbKeyManyToOneElement element : compositeIdElement().getKeyManyToOne()){
				//todo: implement
			}
			return attributeSources;
		}


		@Override
		public String getParentReferenceAttributeName() {
			// composite-id cannot name parent
			return null;
		}

		@Override
		public String getExplicitTuplizerClassName() {
			// composite-id cannot name tuplizer
			return null;
		}

		@Override
		public PropertyGeneration getGeneration() {
			// identifiers have implicit generation
			return null;
		}

		@Override
		public boolean isLazy() {
			return false;
		}

		@Override
		public boolean isIncludedInOptimisticLocking() {
			return false;
		}

		@Override
		public List<RelationalValueSource> relationalValueSources() {
			return null;
		}

		@Override
		public boolean areValuesIncludedInInsertByDefault() {
			return false;
		}

		@Override
		public boolean areValuesIncludedInUpdateByDefault() {
			return false;
		}

		@Override
		public boolean areValuesNullableByDefault() {
			return false;
		}
	}

	private class NonAggregatedCompositeIdentifierSourceImpl implements NonAggregatedCompositeIdentifierSource {
		@Override
		public Class getLookupIdClass() {
			return StringHelper.isEmpty( entityElement().getCompositeId().getClazz() ) ?
					null :
					bindingContext().locateClassByName(
							bindingContext().qualifyClassName( entityElement().getCompositeId().getClazz() )
			);
		}

		@Override
		public String getIdClassPropertyAccessorName() {
			return null;
		}

		@Override
		public List<SingularAttributeSource> getAttributeSourcesMakingUpIdentifier() {
			final List<SingularAttributeSource> attributeSources = new ArrayList<SingularAttributeSource>();
			final JaxbCompositeIdElement compositeId = entityElement().getCompositeId();
			for(final JaxbKeyPropertyElement keyProperty: compositeId.getKeyProperty()) {
				attributeSources.add( new IdentifierKeyAttributeSourceImpl( sourceMappingDocument(), keyProperty ) );
			}
			for(final JaxbKeyManyToOneElement keyProperty : compositeId.getKeyManyToOne()){
//				final AttributeSource attributeSource = buildAttributeSource(
//						keyProperty,
//						null,
//						SingularAttributeBinding.NaturalIdMutability.NOT_NATURAL_ID
//				);
//				if ( ! attributeSource.isSingular() ) {
//					throw new HibernateException( "Only singular attributes are supported for composite identifiers" );
//				}
//				attributeSources.add( (SingularAttributeSource) attributeSource );
				//todo : implement
			}

			return attributeSources;
		}

		@Override
		public IdGenerator getIndividualAttributeIdGenerator(String identifierAttributeName) {
			// for now, return null.  this is that stupid specj bs
			return null;
		}

		@Override
		public IdGenerator getIdentifierGeneratorDescriptor() {
			if ( entityElement().getCompositeId().getGenerator() != null ) {
				final String generatorName = entityElement().getCompositeId().getGenerator().getClazz();
				IdGenerator idGenerator = sourceMappingDocument().getMappingLocalBindingContext()
						.getMetadataImplementor()
						.getIdGenerator( generatorName );
				if ( idGenerator == null ) {
					idGenerator = new IdGenerator(
							getEntityName() + generatorName,
							generatorName,
							Helper.extractParameters( entityElement().getCompositeId().getGenerator().getParam() )
					);
				}
				return idGenerator;
			}
			return null;
		}

		@Override
		public EntityIdentifierNature getNature() {
			return EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
		}

		@Override
		public String getUnsavedValue() {
			return entityElement().getCompositeId().getUnsavedValue().value();
		}

		@Override
		public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
			return entityElement().getCompositeId().getMeta();
		}
	}
}
