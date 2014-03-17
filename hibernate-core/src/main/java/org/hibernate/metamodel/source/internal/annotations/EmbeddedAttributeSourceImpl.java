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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.EmbeddedAttribute;
import org.hibernate.metamodel.source.internal.annotations.attribute.PersistentAttribute;
import org.hibernate.metamodel.source.spi.ComponentAttributeSource;
import org.hibernate.metamodel.source.spi.HibernateTypeSource;
import org.hibernate.metamodel.source.spi.RelationalValueSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;

/**
 * Annotation backed implementation of {@code ComponentAttributeSource}.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 * @author Brett Meyer
 */
public class EmbeddedAttributeSourceImpl
		extends AbstractEmbeddableAdapter
		implements ComponentAttributeSource, AnnotationAttributeSource {
	private final EmbeddedAttribute attribute;
	private final JavaTypeDescriptor embeddableJavaTypeDescriptor;

	private final boolean partOfIdentifier;
	private final boolean partOfPersistentCollection;

	public EmbeddedAttributeSourceImpl(
			EmbeddedAttribute attribute,
			boolean partOfIdentifier,
			boolean partOfPersistentCollection) {
		super( attribute.getEmbeddableTypeMetadata() );

		this.attribute = attribute;
		this.embeddableJavaTypeDescriptor = attribute.getBackingMember().getType().getErasedType();
		this.partOfIdentifier = partOfIdentifier;
		this.partOfPersistentCollection = partOfPersistentCollection;
	}

	@Override
	public PersistentAttribute getAnnotatedAttribute() {
		return attribute;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public Nature getNature() {
		return Nature.COMPOSITE;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public JavaTypeDescriptor getTypeDescriptor() {
		return embeddableJavaTypeDescriptor;
	}

	@Override
	public String getName() {
		return attribute.getName();
	}

	@Override
	public String getExplicitTuplizerClassName() {
		return attribute.getEmbeddableTypeMetadata().getCustomTuplizerClassName();
	}

	@Override
	public String getPropertyAccessorName() {
		// todo : would really rather have binder decipher this...
		return StringHelper.isEmpty( attribute.getAccessorStrategy() )
				? attribute.getAccessType().name().toLowerCase( Locale.ENGLISH )
				: attribute.getAccessorStrategy();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return attribute.getEmbeddableTypeMetadata().getLocalBindingContext();
	}

	@Override
	public String getPath() {
		return attribute.getPath().getFullPath();
	}

	@Override
	public String getParentReferenceAttributeName() {
		return getEmbeddableTypeMetadata().getParentReferencingAttributeName();
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		// not relevant for annotations
		return Collections.emptySet();
	}

	@Override
	public String getContainingTableName() {
		// none, it is defined on the sub-attributes
		return null;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		// none, they are defined on the sub-attributes
		return null;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		// probably need to check for @Target in EmbeddableTypeMetadata (HF)
		return null;
	}

	@Override
	public PropertyGeneration getGeneration() {
		return null;
	}

	@Override
	public boolean isLazy() {
		return false;
	}

	@Override
	public SingularAttributeBinding.NaturalIdMutability getNaturalIdMutability() {
		return attribute.getNaturalIdMutability();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}

	@Override
	public String toString() {
		return "EmbeddedAttributeSourceImpl{role=" + attribute.getRole().getFullPath()
				+ ", embeddable=" + getTypeDescriptor().getName().toString() + "}";
	}

	@Override
	protected SourceHelper.AttributeBuilder getAttributeBuilder() {
		if ( partOfIdentifier ) {
			return SourceHelper.IdentifierPathAttributeBuilder.INSTANCE;
		}

		if ( partOfPersistentCollection ) {
			return SourceHelper.PluralAttributesDisallowedAttributeBuilder.INSTANCE;
		}

		return SourceHelper.StandardAttributeBuilder.INSTANCE;
	}
}
