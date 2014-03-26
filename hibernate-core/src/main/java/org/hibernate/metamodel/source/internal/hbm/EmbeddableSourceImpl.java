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
package org.hibernate.metamodel.source.internal.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbAnyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbArrayElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbBagElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbComponentElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbDynamicComponentElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbKeyPropertyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbListElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbManyToManyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbManyToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbMapElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbNestedCompositeElementElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbOneToManyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbOneToOneElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPrimitiveArrayElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbPropertyElement;
import org.hibernate.metamodel.source.internal.jaxb.hbm.JaxbSetElement;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.EmbeddableSource;
import org.hibernate.metamodel.source.spi.SingularAttributeSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.NaturalIdMutability;

/**
 * @author Steve Ebersole
 */
public class EmbeddableSourceImpl extends AbstractHbmSourceNode implements EmbeddableSource {
	private final EmbeddableJaxbSource embeddableSource;
	private final JavaTypeDescriptor typeDescriptor;

	private final AttributeRole attributeRoleBase;
	private final AttributePath attributePathBase;

	private final String logicalTableName;
	private final NaturalIdMutability naturalIdMutability;

	private final List<AttributeSource> attributeSources;

	public EmbeddableSourceImpl(
			MappingDocument mappingDocument,
			AttributeRole attributeRoleBase,
			AttributePath attributePathBase,
			EmbeddableJaxbSource embeddableJaxbSource,
			String logicalTableName,
			NaturalIdMutability naturalIdMutability) {
		super( mappingDocument );
		this.attributeRoleBase = attributeRoleBase;
		this.attributePathBase = attributePathBase;
		this.embeddableSource = embeddableJaxbSource;
		this.logicalTableName = logicalTableName;
		this.naturalIdMutability = naturalIdMutability;

		this.typeDescriptor = typeDescriptor( embeddableJaxbSource.getClazz() );

		this.attributeSources = buildAttributeSources( embeddableJaxbSource, this );
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return typeDescriptor;
	}

	@Override
	public String getParentReferenceAttributeName() {
		return embeddableSource.findParent();
	}

	@Override
	public String getExplicitTuplizerClassName() {
		return embeddableSource.findTuplizer();
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
	public List<AttributeSource> attributeSources() {
		return attributeSources;
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return bindingContext();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Attribute building

	protected static List<AttributeSource> buildAttributeSources(
			EmbeddableJaxbSource container,
			EmbeddableSourceImpl embeddableSource) {
		final List<AttributeSource> attributeSources = new ArrayList<AttributeSource>();

		for ( JaxbPropertyElement element : container.getPropertyElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbManyToOneElement element : container.getManyToOneElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbOneToOneElement element: container.getOneToOneElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbComponentElement element: container.getComponentElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbNestedCompositeElementElement element: container.getNestedCompositeElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbDynamicComponentElement element: container.getDynamicComponentElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource(element) );
		}

		for ( JaxbAnyElement element: container.getAnyElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbMapElement element: container.getMapElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbSetElement element: container.getSetElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}
		for ( JaxbListElement element: container.getListElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbBagElement element: container.getBagElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbArrayElement element: container.getArrayElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbPrimitiveArrayElement element: container.getPrimitiveArrayElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbKeyPropertyElement element : container.getKeyPropertyElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		for ( JaxbKeyManyToOneElement element : container.getKeyManyToOneElementList() ) {
			attributeSources.add( embeddableSource.buildAttributeSource( element ) );
		}

		return attributeSources;
	}

	private AttributeSource buildAttributeSource(JaxbKeyPropertyElement element) {
		return new IdentifierKeyAttributeSourceImpl( sourceMappingDocument(), this, element );
	}

	private AttributeSource buildAttributeSource(JaxbKeyManyToOneElement element) {
		return new IdentifierKeyManyToOneSourceImpl( sourceMappingDocument(), this, element );
	}

	protected SingularAttributeSource buildAttributeSource(JaxbPropertyElement attributeElement) {
		return new PropertyAttributeSourceImpl(
				sourceMappingDocument(),
				this,
				attributeElement,
				logicalTableName,
				naturalIdMutability
		);
	}

	protected AttributeSource buildAttributeSource(JaxbComponentElement attributeElement) {
		return new EmbeddedAttributeSourceImpl(
				sourceMappingDocument(),
				this,
				attributeElement,
				naturalIdMutability,
				logicalTableName
		);
	}

	private AttributeSource buildAttributeSource(JaxbNestedCompositeElementElement element) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildAttributeSource(JaxbDynamicComponentElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildAttributeSource(JaxbManyToOneElement attributeElement) {
		return new ManyToOneAttributeSourceImpl(
				sourceMappingDocument(),
				this,
				attributeElement,
				logicalTableName,
				naturalIdMutability
		);
	}

	protected AttributeSource buildAttributeSource(JaxbOneToOneElement attributeElement) {
		return new OneToOneAttributeSourceImpl(
				sourceMappingDocument(),
				this,
				attributeElement,
				logicalTableName,
				naturalIdMutability
		);
	}

	protected AttributeSource buildAttributeSource(JaxbAnyElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildAttributeSource(JaxbOneToManyElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildAttributeSource(JaxbManyToManyElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}

	protected AttributeSource buildAttributeSource(JaxbMapElement attributeElement) {
		return new MapSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				this
		);
	}
	protected AttributeSource buildAttributeSource(JaxbSetElement attributeElement) {
		return new SetSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				this
		);
	}
	protected AttributeSource buildAttributeSource(JaxbListElement attributeElement) {
		return new ListSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				this
		);
	}
	protected AttributeSource buildAttributeSource(JaxbBagElement attributeElement) {
		return new BagSourceImpl(
				sourceMappingDocument(),
				attributeElement,
				this
		);
	}
	protected AttributeSource buildAttributeSource(JaxbArrayElement attributeElement) {
		return new ArraySourceImpl(
				sourceMappingDocument(),
				attributeElement,
				this
		);
	}
	protected AttributeSource buildAttributeSource(JaxbPrimitiveArrayElement attributeElement) {
		// todo : implement
		throw new NotYetImplementedException();
	}
}
