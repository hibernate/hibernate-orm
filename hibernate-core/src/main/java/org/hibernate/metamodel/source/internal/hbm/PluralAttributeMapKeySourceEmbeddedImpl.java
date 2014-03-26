/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import java.util.List;

import org.hibernate.metamodel.internal.binder.Binder;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbCompositeIndexElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbCompositeMapKeyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyPropertyElement;
import org.hibernate.metamodel.source.spi.EmbeddableSource;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.PluralAttributeMapKeySourceEmbedded;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;

/**
 * @author Gail Badner
 */
public class PluralAttributeMapKeySourceEmbeddedImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeMapKeySourceEmbedded {

	private final AbstractPluralAttributeSourceImpl pluralAttributeSource;
	private final EmbeddableSourceImpl embeddableSource;


	public PluralAttributeMapKeySourceEmbeddedImpl(
			MappingDocument mappingDocument,
			AbstractPluralAttributeSourceImpl pluralAttributeSource,
			final JaxbCompositeIndexElement compositeIndexElement) {
		this(
				mappingDocument,
				pluralAttributeSource,
				new AbstractEmbeddableJaxbSource() {

					@Override
					public String getClazz() {
						return compositeIndexElement.getClazz();
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
						return compositeIndexElement.getKeyProperty();
					}

					@Override
					public List<JaxbKeyManyToOneElement> getKeyManyToOneElementList() {
						return compositeIndexElement.getKeyManyToOne();
					}
				}
		);
	}

	public PluralAttributeMapKeySourceEmbeddedImpl(
			MappingDocument mappingDocument,
			AbstractPluralAttributeSourceImpl pluralAttributeSource,
			final JaxbCompositeMapKeyElement compositeMapKeyElement) {
		this(
				mappingDocument,
				pluralAttributeSource,

				new AbstractEmbeddableJaxbSource() {

					@Override
					public String getClazz() {
						return compositeMapKeyElement.getClazz();
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
						return compositeMapKeyElement.getKeyProperty();
					}

					@Override
					public List<JaxbKeyManyToOneElement> getKeyManyToOneElementList() {
						return compositeMapKeyElement.getKeyManyToOne();
					}
				}
		);
	}

	private PluralAttributeMapKeySourceEmbeddedImpl(
			MappingDocument mappingDocument,
			AbstractPluralAttributeSourceImpl pluralAttributeSource,
			EmbeddableJaxbSource embeddableJaxbSource) {
		super( mappingDocument );

		this.pluralAttributeSource = pluralAttributeSource;
		this.embeddableSource = new EmbeddableSourceImpl(
				mappingDocument,
				pluralAttributeSource.getAttributeRole().append( "key" ),
				pluralAttributeSource.getAttributePath().append( "key" ),
				embeddableJaxbSource,
				null,
				NaturalIdMutability.NOT_NATURAL_ID
		);
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		return PluralAttributeIndexNature.AGGREGATE;
	}

	@Override
	public EmbeddableSource getEmbeddableSource() {
		return embeddableSource;
	}

	@Override
	public List<Binder.DefaultNamingStrategy> getDefaultNamingStrategies() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return null;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}
}
