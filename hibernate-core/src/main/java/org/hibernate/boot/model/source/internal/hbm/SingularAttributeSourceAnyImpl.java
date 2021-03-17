/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyAssociationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmAnyValueMappingType;
import org.hibernate.boot.model.source.spi.AnyDiscriminatorSource;
import org.hibernate.boot.model.source.spi.AnyKeySource;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.boot.model.source.spi.SingularAttributeNature;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceAny;
import org.hibernate.boot.model.source.spi.ToolingHintContext;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.tuple.GenerationTiming;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeSourceAnyImpl
		extends AbstractHbmSourceNode
		implements SingularAttributeSourceAny {

	private final JaxbHbmAnyAssociationType jaxbAnyMapping;
	private final NaturalIdMutability naturalIdMutability;

	private final AttributePath attributePath;
	private final AttributeRole attributeRole;

	// we don't really know the type of the attribute overall
	private final HibernateTypeSource attributeTypeSource = new HibernateTypeSourceImpl( (String) null );

	private final AnyDiscriminatorSource discriminatorSource;
	private final AnyKeySource keySource;

	private final ToolingHintContext toolingHintContext;

	public SingularAttributeSourceAnyImpl(
			final MappingDocument sourceMappingDocument,
			AttributeSourceContainer container,
			final JaxbHbmAnyAssociationType jaxbAnyMapping,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument );
		this.jaxbAnyMapping = jaxbAnyMapping;
		this.naturalIdMutability = naturalIdMutability;

		this.attributePath = container.getAttributePathBase().append( jaxbAnyMapping.getName() );
		this.attributeRole = container.getAttributeRoleBase().append( jaxbAnyMapping.getName() );


		final List<RelationalValueSource> relationalValueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument,
				logicalTableName,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.ANY;
					}

					@Override
					public String getSourceName() {
						return jaxbAnyMapping.getName();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbAnyMapping.getColumn();
					}
				}
		);

		// the list of relational values should contain 2 or more values:
		//		* the first represents the discriminator
		//		* the rest represent the fk

		if ( relationalValueSources.size() < 2 ) {
			throw new MappingException(
					String.format(
							Locale.ENGLISH,
							"<any name=\"%s\" /> mapping needs to specify 2 or more columns",
							jaxbAnyMapping.getName()
					),
					origin()
			);
		}

		this.discriminatorSource = new AnyDiscriminatorSource() {
			private final HibernateTypeSource typeSource = new HibernateTypeSourceImpl( jaxbAnyMapping.getMetaType() );
			private final RelationalValueSource relationalValueSource = relationalValueSources.get( 0 );
			private final Map<String,String> valueMappings = new HashMap<String, String>();
			{
				for ( JaxbHbmAnyValueMappingType valueMapping : jaxbAnyMapping.getMetaValue() ) {
					valueMappings.put(
							valueMapping.getValue(),
							sourceMappingDocument.qualifyClassName( valueMapping.getClazz() )
					);
				}
			}

			@Override
			public HibernateTypeSource getTypeSource() {
				return typeSource;
			}

			@Override
			public RelationalValueSource getRelationalValueSource() {
				return relationalValueSource;
			}

			@Override
			public Map<String, String> getValueMappings() {
				return valueMappings;
			}

			@Override
			public AttributePath getAttributePath() {
				return attributePath;
			}

			@Override
			public MetadataBuildingContext getBuildingContext() {
				return sourceMappingDocument;
			}
		};

		this.keySource = new AnyKeySource() {
			private final HibernateTypeSource fkTypeSource = new HibernateTypeSourceImpl( jaxbAnyMapping.getIdType() );
			private final List<RelationalValueSource> fkRelationalValueSources = relationalValueSources.subList( 1, relationalValueSources.size() );

			@Override
			public HibernateTypeSource getTypeSource() {
				return fkTypeSource;
			}

			@Override
			public List<RelationalValueSource> getRelationalValueSources() {
				return fkRelationalValueSources;
			}

			@Override
			public AttributePath getAttributePath() {
				return attributePath;
			}

			@Override
			public MetadataBuildingContext getBuildingContext() {
				return sourceMappingDocument;
			}
		};

		toolingHintContext = Helper.collectToolingHints(
				sourceMappingDocument.getToolingHintContext(),
				jaxbAnyMapping
		);
	}

	@Override
	public SingularAttributeNature getSingularAttributeNature() {
		return SingularAttributeNature.ANY;
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.ANY;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public String getName() {
		return jaxbAnyMapping.getName();
	}

	@Override
	public String getXmlNodeName() {
		return jaxbAnyMapping.getNode();
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
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.NEVER;
	}

	@Override
	public Boolean isInsertable() {
		return jaxbAnyMapping.isInsert();
	}

	@Override
	public Boolean isUpdatable() {
		return jaxbAnyMapping.isUpdate();
	}

	@Override
	public boolean isBytecodeLazy() {
		return jaxbAnyMapping.isLazy();
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return attributeTypeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return jaxbAnyMapping.getAccess();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return jaxbAnyMapping.isOptimisticLock();
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return toolingHintContext;
	}

	@Override
	public AnyDiscriminatorSource getDiscriminatorSource() {
		return discriminatorSource;
	}

	@Override
	public AnyKeySource getKeySource() {
		return keySource;
	}

	@Override
	public String getCascadeStyleName() {
		return jaxbAnyMapping.getCascade();
	}

	@Override
	public boolean isLazy() {
		return isBytecodeLazy();
	}
}
