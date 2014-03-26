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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.EntityMode;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbAnyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbCompositeElementElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbNestedCompositeElementElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPropertyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbTuplizerElement;
import org.hibernate.metamodel.source.spi.EmbeddableSource;
import org.hibernate.metamodel.source.spi.PluralAttributeElementSourceEmbedded;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.PluralAttributeElementNature;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class PluralAttributeElementSourceEmbeddedImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeElementSourceEmbedded {

	private final JaxbCompositeElementElement compositeElement;
	private final EmbeddableSourceImpl embeddableSource;
	private final Set<CascadeStyle> cascadeStyles;

	public PluralAttributeElementSourceEmbeddedImpl(
			MappingDocument mappingDocument,
			AbstractPluralAttributeSourceImpl pluralAttributeSource,
			JaxbCompositeElementElement compositeElement,
			String cascadeString) {
		super( mappingDocument );
		this.compositeElement = compositeElement;

		this.embeddableSource = new EmbeddableSourceImpl(
				mappingDocument,
				pluralAttributeSource.getAttributeRole(),
				pluralAttributeSource.getAttributePath(),
				new EmbeddableJaxbSourceImpl( compositeElement ),
				null,
				NaturalIdMutability.NOT_NATURAL_ID
		);

		this.cascadeStyles = Helper.interpretCascadeStyles( cascadeString, bindingContext() );
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.AGGREGATE;
	}

	@Override
	public EmbeddableSource getEmbeddableSource() {
		return embeddableSource;
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		return compositeElement.getMeta();
	}

	public static class EmbeddableJaxbSourceImpl extends AbstractEmbeddableJaxbSource {
		private final JaxbCompositeElementElement compositeElement;

		public EmbeddableJaxbSourceImpl(JaxbCompositeElementElement compositeElement) {
			this.compositeElement = compositeElement;
		}

		@Override
		public String getClazz() {
			return compositeElement.getClazz();
		}

		@Override
		public String findParent() {
			return compositeElement.getParent() != null
					? compositeElement.getParent().getName()
					: null;
		}

		@Override
		public String findTuplizer() {
			if ( compositeElement.getTuplizer() == null ) {
				return null;
			}
			final EntityMode entityMode = StringHelper.isEmpty( compositeElement.getClazz() ) ? EntityMode.MAP : EntityMode.POJO;
			for ( JaxbTuplizerElement tuplizerElement : compositeElement.getTuplizer() ) {
				if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode().value() ) ) {
					return tuplizerElement.getClazz();
				}
			}
			return null;
		}

		@Override
		public List<JaxbPropertyElement> getPropertyElementList() {
			return compositeElement.getProperty();
		}

		@Override
		public List<JaxbAnyElement> getAnyElementList() {
			return compositeElement.getAny();
		}

		@Override
		public List<JaxbNestedCompositeElementElement> getNestedCompositeElementList() {
			return compositeElement.getNestedCompositeElement();
		}

		@Override
		public List<JaxbManyToOneElement> getManyToOneElementList() {
			return compositeElement.getManyToOne();
		}
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return cascadeStyles;
	}
}
