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
package org.hibernate.metamodel.source.internal.annotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.hibernate.metamodel.source.internal.annotations.entity.EntityTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.IdentifiableTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.entity.MappedSuperclassTypeMetadata;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.IdentifiableTypeSource;
import org.hibernate.metamodel.source.spi.JpaCallbackSource;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.hibernate.metamodel.spi.LocalBindingContext;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.xml.spi.Origin;

/**
 * Base class adapting "identifiable types" (entities and mapped-superclasses)
 * from annotation (plus XML overrides) representation to the source
 * representation consumed by the metamodel binder.
 *
 * @author Steve Ebersole
 */
public abstract class IdentifiableTypeSourceAdapter implements IdentifiableTypeSource {
	private final IdentifiableTypeMetadata identifiableTypeMetadata;
	private final EntityHierarchySourceImpl hierarchy;
	private final IdentifiableTypeSourceAdapter superTypeSource;

	private Collection<IdentifiableTypeSource> subclassSources;
	private List<AttributeSource> attributes;

	/**
	 * This form is intended for the root of a hierarchy
	 *
	 * @param identifiableTypeMetadata Metadata about the "identifiable type"
	 * @param hierarchy The hierarchy flyweight
	 */
	protected IdentifiableTypeSourceAdapter(
			IdentifiableTypeMetadata identifiableTypeMetadata,
			EntityHierarchySourceImpl hierarchy,
			boolean isRootEntity) {
		this.identifiableTypeMetadata = identifiableTypeMetadata;
		this.hierarchy = hierarchy;

		// walk up
		this.superTypeSource = walkRootSuperclasses( identifiableTypeMetadata.getSuperType(), hierarchy );
		if ( superTypeSource != null ) {
			superTypeSource.addSubclass( this );
		}

		if ( isRootEntity ) {
			// walk down
			walkSubclasses( identifiableTypeMetadata, this );
		}
	}

	private void addSubclass(IdentifiableTypeSourceAdapter subclassSource) {
		assert subclassSource.identifiableTypeMetadata.getSuperType() == this.identifiableTypeMetadata;
		if ( subclassSources == null ) {
			subclassSources = new ArrayList<IdentifiableTypeSource>();
		}
		subclassSources.add( subclassSource );
	}

	private static IdentifiableTypeSourceAdapter walkRootSuperclasses(
			ManagedTypeMetadata clazz,
			EntityHierarchySourceImpl hierarchy) {
		if ( clazz == null ) {
			return null;
		}

		if ( MappedSuperclassTypeMetadata.class.isInstance( clazz ) ) {
			// IMPORTANT : routing through the root constructor!
			return new MappedSuperclassSourceImpl( (MappedSuperclassTypeMetadata) clazz, hierarchy );
		}
		else {
			throw new UnsupportedOperationException(
					String.format(
							Locale.ENGLISH,
							"Unexpected @Entity [%s] as MappedSuperclass of entity hierarchy",
							clazz.getName()
					)
			);
		}
	}

	private void walkSubclasses(
			IdentifiableTypeMetadata classMetadata,
			IdentifiableTypeSourceAdapter classSource) {
		for ( ManagedTypeMetadata subclass : classMetadata.getSubclasses() ) {
			final IdentifiableTypeSourceAdapter subclassSource;
			if ( MappedSuperclassTypeMetadata.class.isInstance( subclass ) ) {
				subclassSource = new MappedSuperclassSourceImpl(
						(MappedSuperclassTypeMetadata) subclass,
						this.hierarchy,
						classSource
				);
			}
			else if ( this.hierarchy.getHierarchyInheritanceType() == InheritanceType.JOINED ) {
				subclassSource = new JoinedSubclassEntitySourceImpl(
						(EntityTypeMetadata) subclass,
						this.hierarchy,
						classSource
				);
			}
			else {
				subclassSource = new SubclassEntitySourceImpl(
						(EntityTypeMetadata) subclass,
						this.hierarchy,
						classSource
				);
			}
			classSource.addSubclass( subclassSource );

			walkSubclasses( (IdentifiableTypeMetadata) subclass, subclassSource );
		}
	}


	/**
	 * This form is intended for creating subclasses
	 *
	 * @param identifiableTypeMetadata
	 * @param hierarchy
	 * @param superTypeSource
	 */
	protected IdentifiableTypeSourceAdapter(
			IdentifiableTypeMetadata identifiableTypeMetadata,
			EntityHierarchySourceImpl hierarchy,
			IdentifiableTypeSourceAdapter superTypeSource) {
		this.identifiableTypeMetadata = identifiableTypeMetadata;
		this.hierarchy = hierarchy;
		this.superTypeSource = superTypeSource;
	}

	public IdentifiableTypeMetadata getIdentifiableTypeMetadata() {
		return identifiableTypeMetadata;
	}

	public ManagedTypeMetadata getManagedTypeMetadata() {
		return identifiableTypeMetadata;
	}

	@Override
	public Origin getOrigin() {
		return identifiableTypeMetadata.getLocalBindingContext().getOrigin();
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return identifiableTypeMetadata.getLocalBindingContext();
	}

	@Override
	public EntityHierarchySource getHierarchy() {
		return hierarchy;
	}

	@Override
	public String getTypeName() {
		return identifiableTypeMetadata.getName();
	}

	@Override
	public IdentifiableTypeSourceAdapter getSuperType() {
		return superTypeSource;
	}

	@Override
	public Collection<IdentifiableTypeSource> getSubTypes() {
		return subclassSources == null ? Collections.<IdentifiableTypeSource>emptyList() : subclassSources;
	}

	@Override
	public List<JpaCallbackSource> getJpaCallbackClasses() {
		return identifiableTypeMetadata.getJpaCallbacks();
	}

	@Override
	public AttributePath getAttributePathBase() {
		return identifiableTypeMetadata.getAttributePathBase();
	}

	@Override
	public AttributeRole getAttributeRoleBase() {
		return identifiableTypeMetadata.getAttributeRoleBase();
	}

	@Override
	public List<AttributeSource> attributeSources() {
		if ( attributes == null ) {
			attributes = SourceHelper.buildAttributeSources(
					identifiableTypeMetadata,
					SourceHelper.StandardAttributeBuilder.INSTANCE
			);
		}
		return attributes;
	}
}
