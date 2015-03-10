/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmCompositeAttributeType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmDynamicComponentType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNestedCompositeElementType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmToolingHintType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTuplizerType;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.AttributeSourceContainer;
import org.hibernate.boot.model.source.spi.EmbeddableMapping;
import org.hibernate.boot.model.source.spi.EmbeddedAttributeMapping;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.ToolingHintContext;

/**
 * The source information for a singular attribute whose type is composite
 * (embedded in JPA terms).
 *
 * @author Steve Ebersole
 */
class SingularAttributeSourceEmbeddedImpl extends AbstractSingularAttributeSourceEmbeddedImpl {
	private final String xmlNodeName;
	private final boolean insert;
	private final boolean update;
	private final boolean lazy;
	private final boolean optimisticLock;

	public SingularAttributeSourceEmbeddedImpl(
			MappingDocument sourceMappingDocument,
			AttributeSourceContainer parentContainer,
			final JaxbHbmCompositeAttributeType jaxbComponentElement,
			NaturalIdMutability naturalIdMutability,
			String logicalTableName) {
		super(
				sourceMappingDocument,
				parentContainer,
				new EmbeddedAttributeMapping() {
					private final EmbeddableMapping embeddableMapping = new EmbeddableMapping() {
						@Override
						public String getClazz() {
							return jaxbComponentElement.getClazz();
						}

						@Override
						public List<JaxbHbmTuplizerType> getTuplizer() {
							return jaxbComponentElement.getTuplizer();
						}

						@Override
						public String getParent() {
							return jaxbComponentElement.getParent() == null
									? null
									: jaxbComponentElement.getParent().getName();
						}
					};

					@Override
					public List<JaxbHbmToolingHintType> getToolingHints() {
						return jaxbComponentElement.getToolingHints();
					}

					@Override
					public String getName() {
						return jaxbComponentElement.getName();
					}

					@Override
					public String getAccess() {
						return jaxbComponentElement.getAccess();
					}

					@Override
					public boolean isUnique() {
						return jaxbComponentElement.isUnique();
					}

					@Override
					public EmbeddableMapping getEmbeddableMapping() {
						return embeddableMapping;
					}
				},
				jaxbComponentElement.getAttributes(),
				false,
				naturalIdMutability,
				logicalTableName
		);

		this.xmlNodeName = jaxbComponentElement.getNode();
		this.insert = jaxbComponentElement.isInsert();
		this.update = jaxbComponentElement.isUpdate();
		this.lazy = jaxbComponentElement.isLazy();
		this.optimisticLock = jaxbComponentElement.isOptimisticLock();
	}

	public SingularAttributeSourceEmbeddedImpl(
			MappingDocument mappingDocument,
			AttributeSourceContainer parentContainer,
			final JaxbHbmNestedCompositeElementType attributeJaxbMapping,
			NaturalIdMutability naturalIdMutability,
			String logicalTableName) {
		super(
				mappingDocument,
				parentContainer,
				new EmbeddedAttributeMapping() {
					private final EmbeddableMapping embeddableMapping = new EmbeddableMapping() {
						@Override
						public String getClazz() {
							return attributeJaxbMapping.getClazz();
						}

						@Override
						public List<JaxbHbmTuplizerType> getTuplizer() {
							return attributeJaxbMapping.getTuplizer();
						}

						@Override
						public String getParent() {
							return attributeJaxbMapping.getParent() == null
									? null
									: attributeJaxbMapping.getParent().getName();
						}
					};

					@Override
					public List<JaxbHbmToolingHintType> getToolingHints() {
						return Collections.emptyList();
					}

					@Override
					public String getName() {
						return attributeJaxbMapping.getName();
					}

					@Override
					public String getAccess() {
						return attributeJaxbMapping.getAccess();
					}

					@Override
					public boolean isUnique() {
						return false;
					}

					@Override
					public EmbeddableMapping getEmbeddableMapping() {
						return embeddableMapping;
					}
				},
				attributeJaxbMapping.getAttributes(),
				false,
				naturalIdMutability,
				logicalTableName
		);

		this.xmlNodeName = attributeJaxbMapping.getNode();
		this.insert = true;
		this.update = true;
		this.lazy = false;
		this.optimisticLock = true;
	}

	public SingularAttributeSourceEmbeddedImpl(
			MappingDocument sourceMappingDocument,
			AttributeSourceContainer parentContainer,
			final JaxbHbmDynamicComponentType jaxbDynamicEmbeddedMapping,
			NaturalIdMutability naturalIdMutability,
			String logicalTableName) {
		super(
				sourceMappingDocument,
				parentContainer,
				new EmbeddedAttributeMapping() {
					private final EmbeddableMapping embeddableMapping = new EmbeddableMapping() {
						@Override
						public String getClazz() {
							return null;
						}

						@Override
						public List<JaxbHbmTuplizerType> getTuplizer() {
							return Collections.emptyList();
						}

						@Override
						public String getParent() {
							return null;
						}
					};

					@Override
					public boolean isUnique() {
						return jaxbDynamicEmbeddedMapping.isUnique();
					}

					@Override
					public String getName() {
						return jaxbDynamicEmbeddedMapping.getName();
					}

					@Override
					public String getAccess() {
						return jaxbDynamicEmbeddedMapping.getAccess();
					}

					@Override
					public EmbeddableMapping getEmbeddableMapping() {
						return embeddableMapping;
					}

					@Override
					public List<JaxbHbmToolingHintType> getToolingHints() {
						return null;
					}
				},
				jaxbDynamicEmbeddedMapping.getAttributes(),
				true,
				naturalIdMutability,
				logicalTableName
		);

		this.xmlNodeName = jaxbDynamicEmbeddedMapping.getNode();
		this.insert = jaxbDynamicEmbeddedMapping.isInsert();
		this.update = jaxbDynamicEmbeddedMapping.isUpdate();
		this.lazy = false;
		this.optimisticLock = jaxbDynamicEmbeddedMapping.isOptimisticLock();
	}

	@Override
	public XmlElementMetadata getSourceType() {
		return XmlElementMetadata.COMPONENT;
	}

	@Override
	public String getXmlNodeName() {
		return xmlNodeName;
	}

	@Override
	public Boolean isInsertable() {
		return insert;
	}

	@Override
	public Boolean isUpdatable() {
		return update;
	}

	@Override
	public boolean isBytecodeLazy() {
		return lazy;
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
		return optimisticLock;
	}

	@Override
	public ToolingHintContext getToolingHintContext() {
		return getEmbeddableSource().getToolingHintContext();
	}

}
