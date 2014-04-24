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
package org.hibernate.metamodel.source.internal.hbm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.TruthValue;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbClassElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbCompositeIdElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbDiscriminatorElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyPropertyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbMultiTenancyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPolymorphismAttribute;
import org.hibernate.metamodel.source.spi.AggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.source.spi.DiscriminatorSource;
import org.hibernate.metamodel.source.spi.EmbeddableSource;
import org.hibernate.metamodel.source.spi.EmbeddedAttributeSource;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.IdentifierSource;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.source.spi.MapsIdSource;
import org.hibernate.metamodel.source.spi.MultiTenancySource;
import org.hibernate.metamodel.source.spi.NonAggregatedCompositeIdentifierSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.SimpleIdentifierSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.source.spi.SizeSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.source.spi.VersionAttributeSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.binding.InheritanceType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class EntityHierarchySourceImpl implements EntityHierarchySource {
	private static final Logger log = Logger.getLogger( EntityHierarchySourceImpl.class );

	private final RootEntitySourceImpl rootEntitySource;
	private InheritanceType hierarchyInheritanceType = InheritanceType.NO_INHERITANCE;
	private final Caching caching;
	private final Caching naturalIdCaching;

	public EntityHierarchySourceImpl(RootEntitySourceImpl rootEntitySource) {
		this.rootEntitySource = rootEntitySource;
		this.rootEntitySource.injectHierarchy( this );

		this.caching = Helper.createCaching( entityElement().getCache() );
		this.naturalIdCaching = Helper.createNaturalIdCaching(
				rootEntitySource.entityElement().getNaturalIdCache()
		);
	}

	@Override
	public InheritanceType getHierarchyInheritanceType() {
		return hierarchyInheritanceType;
	}

	@Override
	public EntitySource getRoot() {
		return rootEntitySource;
	}

	public void processSubclass(SubclassEntitySourceImpl subclassEntitySource) {
		final InheritanceType inheritanceType = Helper.interpretInheritanceType( subclassEntitySource.entityElement() );
		if ( hierarchyInheritanceType == InheritanceType.NO_INHERITANCE ) {
			hierarchyInheritanceType = inheritanceType;
		}
		else if ( hierarchyInheritanceType != inheritanceType ) {
			throw new MappingException( "Mixed inheritance strategies not supported", subclassEntitySource.getOrigin() );
		}
	}


	protected JaxbClassElement entityElement() {
		return rootEntitySource.entityElement();
	}

	@Override
	public IdentifierSource getIdentifierSource() {
		if ( entityElement().getId() == null && entityElement().getCompositeId() == null ) {
			throw rootEntitySource.getLocalBindingContext().makeMappingException(
					String.format( "Entity [%s] did not define an identifier", rootEntitySource.getEntityName() )
			);
		}

		if ( entityElement().getId() != null ) {
			return new SimpleIdentifierSourceImpl();
		}
		else {
			// if we get here, we should have a composite identifier.  Just need
			// to determine if it is aggregated, or non-aggregated...
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

	protected MappingException makeMappingException(String message) {
		return rootEntitySource.bindingContext().makeMappingException( message );
	}

	protected MappingException makeMappingException(String message, Exception cause) {
		return rootEntitySource.bindingContext().makeMappingException( message, cause );
	}

	@Override
	public VersionAttributeSource getVersionAttributeSource() {
		if ( entityElement().getVersion() != null ) {
			return new VersionAttributeSourceImpl(
					rootEntitySource.sourceMappingDocument(),
					rootEntitySource,
					entityElement().getVersion()
			);
		}
		else if ( entityElement().getTimestamp() != null ) {
			return new TimestampAttributeSourceImpl(
					rootEntitySource.sourceMappingDocument(),
					rootEntitySource,
					entityElement().getTimestamp()
			);
		}
		return null;
	}

	@Override
	public EntityMode getEntityMode() {
		return rootEntitySource.determineEntityMode();
	}

	@Override
	public boolean isMutable() {
		return entityElement().isMutable();
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return JaxbPolymorphismAttribute.EXPLICIT == entityElement().getPolymorphism();
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
					rootEntitySource.sourceMappingDocument().getOrigin()
			);
		}
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public Caching getNaturalIdCaching() {
		return naturalIdCaching;
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
				SizeSource sizeSource = Helper.createSizeSourceIfMapped( discriminatorElement.getLength(), null, null );
				if ( StringHelper.isNotEmpty( discriminatorElement.getColumnAttribute() ) || sizeSource != null ) {
					return new ColumnAttributeSourceImpl(
							rootEntitySource.sourceMappingDocument(),
							null, // root table
							discriminatorElement.getColumnAttribute(),
							sizeSource,
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE,
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE,
							discriminatorElement.isNotNull() ? TruthValue.FALSE : TruthValue.TRUE
					);
				}
				else if ( StringHelper.isNotEmpty( discriminatorElement.getFormulaAttribute() ) ) {
					return new FormulaImpl(
							rootEntitySource.sourceMappingDocument(),
							null,
							discriminatorElement.getFormulaAttribute()
					);
				}
				else if ( discriminatorElement.getColumn() != null ) {
					return new ColumnSourceImpl(
							rootEntitySource.sourceMappingDocument(),
							null, // root table
							discriminatorElement.getColumn(),
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE,
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE
					);
				}
				else if ( StringHelper.isNotEmpty( discriminatorElement.getFormula() ) ) {
					return new FormulaImpl(
							rootEntitySource.sourceMappingDocument(),
							null,
							discriminatorElement.getFormula()
					);
				}
				else {
					log.debug( "No source for discriminator column/formula found" );
					return new ColumnAttributeSourceImpl(
							rootEntitySource.sourceMappingDocument(),
							null, // root table
							"class", // the default discriminator column name per-legacy hbm binding
							sizeSource,
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE,
							discriminatorElement.isInsert() ? TruthValue.TRUE : TruthValue.FALSE,
							discriminatorElement.isNotNull() ? TruthValue.FALSE : TruthValue.TRUE
					);
//					throw makeMappingException( "could not determine source of discriminator mapping" );
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
							rootEntitySource.sourceMappingDocument(),
							null, // root table
							jaxbMultiTenancy.getColumnAttribute(),
							null,
							TruthValue.TRUE,
							TruthValue.FALSE
					);
				}
				else if ( StringHelper.isNotEmpty( jaxbMultiTenancy.getFormulaAttribute() ) ) {
					return new FormulaImpl(
							rootEntitySource.sourceMappingDocument(),
							null,
							jaxbMultiTenancy.getFormulaAttribute()
					);
				}
				else if ( jaxbMultiTenancy.getColumn() != null ) {
					return new ColumnSourceImpl(
							rootEntitySource.sourceMappingDocument(),
							null, // root table
							jaxbMultiTenancy.getColumn(),
							TruthValue.TRUE,
							TruthValue.FALSE
					);
				}
				else if ( StringHelper.isNotEmpty( jaxbMultiTenancy.getFormula() ) ) {
					return new FormulaImpl(
							rootEntitySource.sourceMappingDocument(),
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
			return new SingularIdentifierAttributeSourceImpl( sourceMappingDocument(), rootEntitySource, entityElement().getId() );
		}

		@Override
		public IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor() {
			if ( entityElement().getId().getGenerator() != null ) {
				final String generatorName = entityElement().getId().getGenerator().getClazz();
				IdentifierGeneratorDefinition identifierGeneratorDefinition = sourceMappingDocument().getMappingLocalBindingContext()
						.getMetadataCollector()
						.getIdGenerator( generatorName );
				if ( identifierGeneratorDefinition == null ) {
					identifierGeneratorDefinition = new IdentifierGeneratorDefinition(
							getEntityName() + generatorName,
							generatorName,
							Helper.extractParameters( entityElement().getId().getGenerator().getParam() )
					);
				}
				return identifierGeneratorDefinition;
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
		public Collection<? extends ToolingHintSource> getToolingHintSources() {
			return entityElement().getId().getMeta();
		}
	}

	private class AggregatedCompositeIdentifierSourceImpl implements AggregatedCompositeIdentifierSource {
		private final CompositeIdentifierEmbeddedAttributeSourceImpl componentAttributeSource
				= new CompositeIdentifierEmbeddedAttributeSourceImpl();

		@Override
		public EmbeddedAttributeSource getIdentifierAttributeSource() {
			return componentAttributeSource;
		}

		@Override
		public List<MapsIdSource> getMapsIdSources() {
			return Collections.emptyList();
		}

		@Override
		public IdentifierGeneratorDefinition getIndividualAttributeIdGenerator(String identifierAttributeName) {
			// for now, return null.  this is that stupid specj bs
			return null;
		}

		@Override
		public IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor() {
			if ( entityElement().getCompositeId().getGenerator() != null ) {
				final String generatorName = entityElement().getCompositeId().getGenerator().getClazz();
				IdentifierGeneratorDefinition identifierGeneratorDefinition = sourceMappingDocument().getMappingLocalBindingContext()
						.getMetadataCollector()
						.getIdGenerator( generatorName );
				if ( identifierGeneratorDefinition == null ) {
					identifierGeneratorDefinition = new IdentifierGeneratorDefinition(
							getEntityName() + generatorName,
							generatorName,
							Helper.extractParameters( entityElement().getCompositeId().getGenerator().getParam() )
					);
				}
				return identifierGeneratorDefinition;
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
		public Collection<? extends ToolingHintSource> getToolingHintSources() {
			return entityElement().getId().getMeta();
		}
	}

	private class CompositeIdentifierEmbeddedAttributeSourceImpl extends AbstractEmbeddedAttributeSourceImpl {
		private final List<JaxbKeyPropertyElement> keyPropertyElementList;
		private final List<JaxbKeyManyToOneElement> keyManyToOneElementList;

		protected CompositeIdentifierEmbeddedAttributeSourceImpl() {
			super(
					EntityHierarchySourceImpl.this.sourceMappingDocument(),
					rootEntitySource,
					rootEntitySource.getAttributeRoleBase().append( "id" ),
					rootEntitySource.getAttributePathBase().append( "id" ),
					entityElement().getCompositeId(),
					new EmbeddableJaxbSourceImpl( entityElement().getCompositeId() ),
					NaturalIdMutability.NOT_NATURAL_ID,
					null
			);

			this.keyPropertyElementList = new ArrayList<JaxbKeyPropertyElement>();
			this.keyManyToOneElementList = new ArrayList<JaxbKeyManyToOneElement>();

			final JaxbCompositeIdElement compositeIdElement = entityElement().getCompositeId();
			for ( final Object obj : compositeIdElement.getKeyPropertyOrKeyManyToOne() ) {
				if ( JaxbKeyPropertyElement.class.isInstance( obj ) ) {
					keyPropertyElementList.add( JaxbKeyPropertyElement.class.cast( obj ) );
				}
				else if ( JaxbKeyManyToOneElement.class.isInstance( obj ) ) {
					keyManyToOneElementList.add( JaxbKeyManyToOneElement.class.cast( obj ) );
				}
			}
		}

		@Override
		public boolean isLazy() {
			return false;
		}

		@Override
		public AttributePath getAttributePath() {
			return getEmbeddableSource().getAttributePathBase();
		}

		@Override
		public AttributeRole getAttributeRole() {
			return getEmbeddableSource().getAttributeRoleBase();
		}

		@Override
		public boolean isIncludedInOptimisticLocking() {
			return false;
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
		private final List<SingularAttributeSource> attributeSources;
		private final EmbeddableSource idClassSource;

		private NonAggregatedCompositeIdentifierSourceImpl() {
			this.attributeSources = new ArrayList<SingularAttributeSource>();
			collectCompositeIdAttributes( attributeSources, rootEntitySource );

			// NOTE : the HBM support for IdClass is very limited.  Essentially
			// we assume that all identifier attributes occur in the IdClass
			// using the same name and type.
			this.idClassSource = interpretIdClass();
		}

		private EmbeddableSource interpretIdClass() {
			final JaxbCompositeIdElement compositeId = entityElement().getCompositeId();
			// if <composite-id/> is null here we have much bigger problems :)

			final String className = compositeId.getClazz();
			if ( StringHelper.isEmpty( className ) ) {
				return null;
			}

			return new IdClassSource( rootEntitySource.getLocalBindingContext().typeDescriptor( className ) );
		}

		@Override
		public List<SingularAttributeSource> getAttributeSourcesMakingUpIdentifier() {
			return attributeSources;
		}


		@Override
		public EmbeddableSource getIdClassSource() {
			return idClassSource;
		}

		@Override
		public IdentifierGeneratorDefinition getIndividualAttributeIdGenerator(String identifierAttributeName) {
			// for now, return null.  this is that stupid specj bs
			return null;
		}

		@Override
		public IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor() {
			if ( entityElement().getCompositeId().getGenerator() != null ) {
				final String generatorName = entityElement().getCompositeId().getGenerator().getClazz();
				IdentifierGeneratorDefinition identifierGeneratorDefinition = sourceMappingDocument().getMappingLocalBindingContext()
						.getMetadataCollector()
						.getIdGenerator( generatorName );
				if ( identifierGeneratorDefinition == null ) {
					identifierGeneratorDefinition = new IdentifierGeneratorDefinition(
							getEntityName() + generatorName,
							generatorName,
							Helper.extractParameters( entityElement().getCompositeId().getGenerator().getParam() )
					);
				}
				return identifierGeneratorDefinition;
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
		public Collection<? extends ToolingHintSource> getToolingHintSources() {
			return entityElement().getCompositeId().getMeta();
		}
	}

	@SuppressWarnings("unchecked")
	private void collectCompositeIdAttributes(
			List attributeSources,
			AttributeSourceContainer attributeSourceContainer) {
		final JaxbCompositeIdElement compositeId = entityElement().getCompositeId();
		final List list = compositeId.getKeyPropertyOrKeyManyToOne();
		for ( final Object obj : list ) {
			if ( JaxbKeyPropertyElement.class.isInstance( obj ) ) {
				JaxbKeyPropertyElement key = JaxbKeyPropertyElement.class.cast( obj );
				attributeSources.add(
						new IdentifierKeyAttributeSourceImpl(
								sourceMappingDocument(),
								attributeSourceContainer,
								key
						)
				);
			}
			if ( JaxbKeyManyToOneElement.class.isInstance( obj ) ) {
				JaxbKeyManyToOneElement key = JaxbKeyManyToOneElement.class.cast( obj );
				attributeSources.add(
						new IdentifierKeyManyToOneSourceImpl(
								sourceMappingDocument(),
								attributeSourceContainer,
								key
						)
				);
			}
		}
	}

	private class IdClassSource implements EmbeddableSource {
		private final JavaTypeDescriptor idClassDescriptor;
		private final List<AttributeSource> attributeSources;

		private IdClassSource(JavaTypeDescriptor idClassDescriptor) {
			this.idClassDescriptor = idClassDescriptor;
			this.attributeSources = new ArrayList<AttributeSource>();
			collectCompositeIdAttributes( attributeSources, this );
		}
		@Override
		public JavaTypeDescriptor getTypeDescriptor() {
			return idClassDescriptor;
		}

		@Override
		public String getParentReferenceAttributeName() {
			return null;
		}

		@Override
		public String getExplicitTuplizerClassName() {
			return null;
		}

		@Override
		public AttributePath getAttributePathBase() {
			return rootEntitySource.getAttributePathBase().append( "<IdClass>" );
		}

		@Override
		public AttributeRole getAttributeRoleBase() {
			return rootEntitySource.getAttributeRoleBase().append( "<IdClass>" );
		}

		@Override
		public List<AttributeSource> attributeSources() {
			return attributeSources;
		}

		@Override
		public LocalBindingContext getLocalBindingContext() {
			return rootEntitySource.getLocalBindingContext();
		}
	}

	public static class EmbeddableJaxbSourceImpl extends AbstractEmbeddableJaxbSource {
		private final JaxbCompositeIdElement compositeIdElement;

		private final List<JaxbKeyPropertyElement> keyPropertyElementList;
		private final List<JaxbKeyManyToOneElement> keyManyToOneElementList;

		public EmbeddableJaxbSourceImpl(JaxbCompositeIdElement compositeIdElement) {
			this.compositeIdElement = compositeIdElement;

			this.keyPropertyElementList = new ArrayList<JaxbKeyPropertyElement>();
			this.keyManyToOneElementList = new ArrayList<JaxbKeyManyToOneElement>();

			for ( final Object obj : compositeIdElement.getKeyPropertyOrKeyManyToOne() ) {
				if ( JaxbKeyPropertyElement.class.isInstance( obj ) ) {
					keyPropertyElementList.add( JaxbKeyPropertyElement.class.cast( obj ) );
				}
				else if ( JaxbKeyManyToOneElement.class.isInstance( obj ) ) {
					keyManyToOneElementList.add( JaxbKeyManyToOneElement.class.cast( obj ) );
				}
			}
		}

		@Override
		public String getClazz() {
			return compositeIdElement.getClazz();
		}

		@Override
		public String findParent() {
			return null;
		}

		@Override
		public String findTuplizer() {
			return null;
		}

		@Override
		public List<JaxbKeyPropertyElement> getKeyPropertyElementList() {
			return keyPropertyElementList;
		}

		@Override
		public List<JaxbKeyManyToOneElement> getKeyManyToOneElementList() {
			return keyManyToOneElementList;
		}

	}

	private String getEntityName() {
		return rootEntitySource.getEntityName();
	}

	private MappingDocument sourceMappingDocument() {
		return rootEntitySource.sourceMappingDocument();
	}
}
