/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.EntityMode;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbAnyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbArrayElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbBagElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbComponentElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbDynamicComponentElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbListElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbMapElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbOneToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPrimitiveArrayElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPropertiesElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPropertyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbSetElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbTuplizerElement;
import org.hibernate.metamodel.source.spi.AttributeSourceContainer;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.NaturalIdMutability;

/**
 * The source information for a singular attribute whose type is composite
 * (embedded in JPA terms).
 *
 * @author Steve Ebersole
 */
class EmbeddedAttributeSourceImpl extends AbstractEmbeddedAttributeSourceImpl {
	public EmbeddedAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			AttributeSourceContainer parentContainer,
			JaxbComponentElement jaxbComponentElement,
			NaturalIdMutability naturalIdMutability,
			String logicalTableName) {
		super(
				sourceMappingDocument,
				parentContainer,
				parentContainer.getAttributeRoleBase().append( jaxbComponentElement.getName() ),
				parentContainer.getAttributePathBase().append( jaxbComponentElement.getName() ),
				jaxbComponentElement,
				new EmbeddableJaxbSourceImpl( jaxbComponentElement ),
				naturalIdMutability,
				logicalTableName
		);
	}

	@Override
	protected JaxbComponentElement jaxbComponentSourceElement() {
		return (JaxbComponentElement) super.jaxbComponentSourceElement();
	}

	@Override
	public boolean isLazy() {
		return jaxbComponentSourceElement().isLazy();
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
		return jaxbComponentSourceElement().isOptimisticLock();
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return jaxbComponentSourceElement().isInsert();
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return jaxbComponentSourceElement().isUpdate();
	}

	public static class EmbeddableJaxbSourceImpl extends AbstractEmbeddableJaxbSource {
		private final JaxbComponentElement jaxbComponentElement;

		public EmbeddableJaxbSourceImpl(JaxbComponentElement jaxbComponentElement) {
			this.jaxbComponentElement = jaxbComponentElement;
		}

		@Override
		public String getClazz() {
			return jaxbComponentElement.getClazz();
		}

		@Override
		public String findParent() {
			return jaxbComponentElement.getParent() == null
					? null
					: jaxbComponentElement.getParent().getName();
		}

		@Override
		public String findTuplizer() {
			if ( jaxbComponentElement.getTuplizer() == null ) {
				return null;
			}

			final EntityMode entityMode = StringHelper.isEmpty( jaxbComponentElement.getClazz() )
					? EntityMode.MAP
					: EntityMode.POJO;

			for ( JaxbTuplizerElement tuplizerElement : jaxbComponentElement.getTuplizer() ) {
				if ( entityMode == EntityMode.parse( tuplizerElement.getEntityMode().value() ) ) {
					return tuplizerElement.getClazz();
				}
			}
			return null;
		}

		@Override
		public List<JaxbPropertyElement> getPropertyElementList() {
			return jaxbComponentElement.getProperty();
		}

		@Override
		public List<JaxbManyToOneElement> getManyToOneElementList() {
			return jaxbComponentElement.getManyToOne();
		}

		@Override
		public List<JaxbOneToOneElement> getOneToOneElementList() {
			return jaxbComponentElement.getOneToOne();
		}

		@Override
		public List<JaxbComponentElement> getComponentElementList() {
			return jaxbComponentElement.getComponent();
		}

		@Override
		public List<JaxbDynamicComponentElement> getDynamicComponentElementList() {
			return jaxbComponentElement.getDynamicComponent();
		}

		@Override
		public List<JaxbPropertiesElement> getPropertiesElementList() {
			return jaxbComponentElement.getProperties();
		}

		@Override
		public List<JaxbAnyElement> getAnyElementList() {
			return jaxbComponentElement.getAny();
		}

		@Override
		public List<JaxbMapElement> getMapElementList() {
			return jaxbComponentElement.getMap();
		}

		@Override
		public List<JaxbSetElement> getSetElementList() {
			return jaxbComponentElement.getSet();
		}

		@Override
		public List<JaxbListElement> getListElementList() {
			return jaxbComponentElement.getList();
		}

		@Override
		public List<JaxbBagElement> getBagElementList() {
			return jaxbComponentElement.getBag();
		}

		@Override
		public List<JaxbArrayElement> getArrayElementList() {
			return jaxbComponentElement.getArray();
		}

		@Override
		public List<JaxbPrimitiveArrayElement> getPrimitiveArrayElementList() {
			return jaxbComponentElement.getPrimitiveArray();
		}
	}
}
