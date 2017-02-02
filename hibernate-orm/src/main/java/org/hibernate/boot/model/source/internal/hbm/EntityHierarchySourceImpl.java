/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmEntityDiscriminatorType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmGeneratorSpecificationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMultiTenancyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmPolymorphismEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmRootEntityType;
import org.hibernate.boot.model.Caching;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.source.spi.DiscriminatorSource;
import org.hibernate.boot.model.source.spi.EntityHierarchySource;
import org.hibernate.boot.model.source.spi.EntityNamingSource;
import org.hibernate.boot.model.source.spi.IdentifierSource;
import org.hibernate.boot.model.source.spi.InheritanceType;
import org.hibernate.boot.model.source.spi.MultiTenancySource;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.SizeSource;
import org.hibernate.boot.model.source.spi.VersionAttributeSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.StringHelper;

/**
 * Models an entity hierarchy as defined by {@code hbm.xml} documents
 *
 * @author Steve Ebersole
 */
public class EntityHierarchySourceImpl implements EntityHierarchySource {
	private final RootEntitySourceImpl rootEntitySource;

	private final IdentifierSource identifierSource;
	private final VersionAttributeSource versionAttributeSource;
	private final DiscriminatorSource discriminatorSource;
	private final MultiTenancySource multiTenancySource;

	private final Caching caching;
	private final Caching naturalIdCaching;

	private InheritanceType hierarchyInheritanceType = InheritanceType.NO_INHERITANCE;

	private Set<String> collectedEntityNames = new HashSet<String>();

	public EntityHierarchySourceImpl(RootEntitySourceImpl rootEntitySource) {
		this.rootEntitySource = rootEntitySource;
		this.rootEntitySource.injectHierarchy( this );

		this.identifierSource = interpretIdentifierSource( rootEntitySource );
		this.versionAttributeSource = interpretVersionSource( rootEntitySource );
		this.discriminatorSource = interpretDiscriminatorSource( rootEntitySource );
		this.multiTenancySource = interpretMultiTenancySource( rootEntitySource );

		this.caching = Helper.createCaching( entityElement().getCache() );
		this.naturalIdCaching = Helper.createNaturalIdCaching(
				rootEntitySource.jaxbEntityMapping().getNaturalIdCache()
		);

		collectedEntityNames.add( rootEntitySource.getEntityNamingSource().getEntityName() );
	}

	private static IdentifierSource interpretIdentifierSource(RootEntitySourceImpl rootEntitySource) {
		if ( rootEntitySource.jaxbEntityMapping().getId() == null
				&& rootEntitySource.jaxbEntityMapping().getCompositeId() == null ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"Entity [%s] did not define an identifier",
							rootEntitySource.getEntityNamingSource().getEntityName()
					),
					rootEntitySource.origin()
			);
		}

		if ( rootEntitySource.jaxbEntityMapping().getId() != null ) {
			return new IdentifierSourceSimpleImpl( rootEntitySource );
		}
		else {
			// if we get here, we should have a composite identifier.  Just need
			// to determine if it is aggregated, or non-aggregated...
			if ( StringHelper.isEmpty( rootEntitySource.jaxbEntityMapping().getCompositeId().getName() ) ) {
				if ( rootEntitySource.jaxbEntityMapping().getCompositeId().isMapped()
						&& StringHelper.isEmpty( rootEntitySource.jaxbEntityMapping().getCompositeId().getClazz() ) ) {
					throw new MappingException(
							"mapped composite identifier must name component class to use.",
							rootEntitySource.origin()
					);
				}
				return new IdentifierSourceNonAggregatedCompositeImpl( rootEntitySource );
			}
			else {
				if ( rootEntitySource.jaxbEntityMapping().getCompositeId().isMapped() ) {
					throw new MappingException(
							"cannot combine mapped=\"true\" with specified name",
							rootEntitySource.origin()
					);
				}
				return new IdentifierSourceAggregatedCompositeImpl( rootEntitySource );
			}
		}
	}

	private static VersionAttributeSource interpretVersionSource(RootEntitySourceImpl rootEntitySource) {
		final JaxbHbmRootEntityType entityElement = rootEntitySource.jaxbEntityMapping();
		if ( entityElement.getVersion() != null ) {
			return new VersionAttributeSourceImpl(
					rootEntitySource.sourceMappingDocument(),
					rootEntitySource,
					entityElement.getVersion()
			);
		}
		else if ( entityElement.getTimestamp() != null ) {
			return new TimestampAttributeSourceImpl(
					rootEntitySource.sourceMappingDocument(),
					rootEntitySource,
					entityElement.getTimestamp()
			);
		}
		return null;
	}

	private static DiscriminatorSource interpretDiscriminatorSource(final RootEntitySourceImpl rootEntitySource) {
		final JaxbHbmEntityDiscriminatorType jaxbDiscriminatorMapping =
				rootEntitySource.jaxbEntityMapping().getDiscriminator();

		if ( jaxbDiscriminatorMapping == null ) {
			return null;
		}

		final RelationalValueSource relationalValueSource = RelationalValueSourceHelper.buildValueSource(
				rootEntitySource.sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.DISCRIMINATOR;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public SizeSource getSizeSource() {
						return Helper.interpretSizeSource(
								jaxbDiscriminatorMapping.getLength(),
								(Integer) null,
								null
						);
					}

					@Override
					public String getFormulaAttribute() {
						return jaxbDiscriminatorMapping.getFormulaAttribute();
					}

					@Override
					public String getColumnAttribute() {
						return jaxbDiscriminatorMapping.getColumnAttribute();
					}

					private List columnOrFormulas;
					@Override
					public List getColumnOrFormulaElements() {
						if ( columnOrFormulas == null ) {
							if ( jaxbDiscriminatorMapping.getColumn() != null ) {
								if ( jaxbDiscriminatorMapping.getFormula() != null ) {
									throw new MappingException(
											String.format(
													Locale.ENGLISH,
													"discriminator mapping [%s] named both <column/> and <formula/>, but only one or other allowed",
													rootEntitySource.getEntityNamingSource().getEntityName()
											),
											rootEntitySource.sourceMappingDocument().getOrigin()
									);
								}
								else {
									columnOrFormulas = Collections.singletonList( jaxbDiscriminatorMapping.getColumn() );
								}
							}
							else {
								if ( jaxbDiscriminatorMapping.getFormula() != null ) {
									columnOrFormulas = Collections.singletonList( jaxbDiscriminatorMapping.getFormula() );
								}
								else {
									columnOrFormulas = Collections.emptyList();
								}
							}
						}
						return columnOrFormulas;
					}

					@Override
					public Boolean isNullable() {
						return !jaxbDiscriminatorMapping.isNotNull();
					}
				}
		);

		return new DiscriminatorSource() {
			@Override
			public EntityNaming getEntityNaming() {
				return rootEntitySource.getEntityNamingSource();
			}

			@Override
			public MetadataBuildingContext getBuildingContext() {
				return rootEntitySource.metadataBuildingContext();
			}

			@Override
			public RelationalValueSource getDiscriminatorRelationalValueSource() {
				return relationalValueSource;
			}

			@Override
			public String getExplicitHibernateTypeName() {
				return jaxbDiscriminatorMapping.getType();
			}

			@Override
			public boolean isForced() {
				return jaxbDiscriminatorMapping.isForce();
			}

			@Override
			public boolean isInserted() {
				return jaxbDiscriminatorMapping.isInsert();
			}
		};
	}

	private static MultiTenancySource interpretMultiTenancySource(final RootEntitySourceImpl rootEntitySource) {
		final JaxbHbmMultiTenancyType jaxbMultiTenancy = rootEntitySource.jaxbEntityMapping().getMultiTenancy();
		if ( jaxbMultiTenancy == null ) {
			return null;
		}

		final RelationalValueSource relationalValueSource = RelationalValueSourceHelper.buildValueSource(
				rootEntitySource.sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.MULTI_TENANCY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getFormulaAttribute() {
						return jaxbMultiTenancy.getFormulaAttribute();
					}

					@Override
					public String getColumnAttribute() {
						return jaxbMultiTenancy.getColumnAttribute();
					}
					private List columnOrFormulas;
					@Override
					public List getColumnOrFormulaElements() {
						if ( columnOrFormulas == null ) {
							if ( jaxbMultiTenancy.getColumn() != null ) {
								if ( jaxbMultiTenancy.getFormula() != null ) {
									throw new MappingException(
											String.format(
													Locale.ENGLISH,
													"discriminator mapping [%s] named both <column/> and <formula/>, but only one or other allowed",
													rootEntitySource.getEntityNamingSource().getEntityName()
											),
											rootEntitySource.sourceMappingDocument().getOrigin()
									);
								}
								else {
									columnOrFormulas = Collections.singletonList( jaxbMultiTenancy.getColumn() );
								}
							}
							else {
								if ( jaxbMultiTenancy.getFormula() != null ) {
									columnOrFormulas = Collections.singletonList( jaxbMultiTenancy.getColumn() );
								}
								else {
									columnOrFormulas = Collections.emptyList();
								}
							}
						}
						return columnOrFormulas;
					}

					@Override
					public Boolean isNullable() {
						return false;
					}
				}
		);

		return new MultiTenancySource() {
			@Override
			public RelationalValueSource getRelationalValueSource() {
				return relationalValueSource;
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

	@Override
	public InheritanceType getHierarchyInheritanceType() {
		return hierarchyInheritanceType;
	}

	@Override
	public RootEntitySourceImpl getRoot() {
		return rootEntitySource;
	}

	public void processSubclass(SubclassEntitySourceImpl subclassEntitySource) {
		final InheritanceType inheritanceType = Helper.interpretInheritanceType( subclassEntitySource.jaxbEntityMapping() );
		if ( hierarchyInheritanceType == InheritanceType.NO_INHERITANCE ) {
			hierarchyInheritanceType = inheritanceType;
		}
		else if ( hierarchyInheritanceType != inheritanceType ) {
			throw new MappingException( "Mixed inheritance strategies not supported", subclassEntitySource.getOrigin() );
		}

		collectedEntityNames.add( subclassEntitySource.getEntityNamingSource().getEntityName() );
	}


	protected JaxbHbmRootEntityType entityElement() {
		return rootEntitySource.jaxbEntityMapping();
	}

	@Override
	public IdentifierSource getIdentifierSource() {
		return identifierSource;
	}

	@Override
	public VersionAttributeSource getVersionAttributeSource() {
		return versionAttributeSource;
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
		return JaxbHbmPolymorphismEnum.EXPLICIT == entityElement().getPolymorphism();
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
		return entityElement().getOptimisticLock();
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
		return discriminatorSource;
	}

	@Override
	public MultiTenancySource getMultiTenancySource() {
		return multiTenancySource;
	}

	/**
	 * Package-protected to allow IdentifierSource implementations to access it.
	 *
	 * @param mappingDocument The source mapping document
	 * @param entityNaming The entity naming
	 * @param jaxbGeneratorMapping The identifier generator mapping
	 *
	 * @return The collected information.
	 */
	static IdentifierGeneratorDefinition interpretGeneratorDefinition(
			MappingDocument mappingDocument,
			EntityNamingSource entityNaming,
			JaxbHbmGeneratorSpecificationType jaxbGeneratorMapping) {
		if ( jaxbGeneratorMapping == null ) {
			return null;
		}

		final String generatorName = jaxbGeneratorMapping.getClazz();
		IdentifierGeneratorDefinition identifierGeneratorDefinition = mappingDocument.getMetadataCollector()
				.getIdentifierGenerator( generatorName );
		if ( identifierGeneratorDefinition == null ) {
			identifierGeneratorDefinition = new IdentifierGeneratorDefinition(
					entityNaming.getEntityName() + '.' + generatorName,
					generatorName,
					Helper.extractParameters( jaxbGeneratorMapping.getConfigParameters() )
			);
		}
		return identifierGeneratorDefinition;
	}

	public Set<String> getContainedEntityNames() {
		return collectedEntityNames;
	}
}
