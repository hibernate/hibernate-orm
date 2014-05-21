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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import java.util.ArrayList;
import java.util.Set;

import javax.persistence.AccessType;
import javax.persistence.CascadeType;

import org.hibernate.engine.FetchStyle;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.reflite.spi.MemberDescriptor;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.CompositeAttributeTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.attribute.type.HibernateTypeResolver;
import org.hibernate.metamodel.source.internal.annotations.entity.ManagedTypeMetadata;
import org.hibernate.metamodel.source.internal.annotations.util.AssociationHelper;
import org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.spi.AttributePath;
import org.hibernate.metamodel.spi.AttributeRole;
import org.jboss.jandex.AnnotationInstance;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class SingularAssociationAttribute
		extends AbstractSingularAttribute
		implements FetchableAttribute, AssociationAttribute {

	private final String target;
	private String mappedByAttributeName;
	private final boolean isInverse;
	private final Set<CascadeType> jpaCascadeTypes;
	private final Set<org.hibernate.annotations.CascadeType> hibernateCascadeTypes;
	private final boolean isOrphanRemoval;
	private final boolean ignoreNotFound;
	private final boolean isOptional;
	private final boolean isUnWrapProxy;

	private final FetchStyle fetchStyle;
	private final boolean isLazy;

	private final AnnotationInstance joinTableAnnotation;
	private ArrayList<Column> joinColumnValues = new ArrayList<Column>();
	private ArrayList<Column> inverseJoinColumnValues = new ArrayList<Column>();

	private final AnnotationInstance mapsIdAnnotation;

	private final boolean hasPrimaryKeyJoinColumn;

	private AttributeTypeResolver resolver;

	public SingularAssociationAttribute(
			ManagedTypeMetadata container,
			String attributeName,
			AttributePath attributePath,
			AttributeRole attributeRole,
			MemberDescriptor backingMember,
			Nature attributeNature,
			AccessType accessType,
			String accessorStrategy) {
		super(
				container,
				attributeName,
				attributePath,
				attributeRole,
				backingMember,
				attributeNature,
				accessType,
				accessorStrategy
		);

		final AnnotationInstance associationAnnotation = backingMember.getAnnotations().get( attributeNature.getAnnotationDotName() );

		this.target = AssociationHelper.INSTANCE.determineTarget(
				backingMember,
				associationAnnotation,
				backingMember.getType().getErasedType(),
				getContext()
		);

		this.mappedByAttributeName = AssociationHelper.INSTANCE.determineMappedByAttributeName( associationAnnotation );

		this.fetchStyle = AssociationHelper.INSTANCE.determineFetchStyle( backingMember );
		this.isLazy = AssociationHelper.INSTANCE.determineWhetherIsLazy(
				associationAnnotation,
				backingMember.getAnnotations().get( HibernateDotNames.LAZY_TO_ONE ),
				backingMember,
				fetchStyle,
				false
		);

		this.isOptional = AssociationHelper.INSTANCE.determineOptionality( associationAnnotation );
		this.isUnWrapProxy = AssociationHelper.INSTANCE.determineWhetherToUnwrapProxy( backingMember );

		this.jpaCascadeTypes = AssociationHelper.INSTANCE.determineCascadeTypes( associationAnnotation );
		this.hibernateCascadeTypes = AssociationHelper.INSTANCE.determineHibernateCascadeTypes( backingMember );
		this.isOrphanRemoval = AssociationHelper.INSTANCE.determineOrphanRemoval( associationAnnotation );
		this.ignoreNotFound = AssociationHelper.INSTANCE.determineWhetherToIgnoreNotFound( backingMember );

		this.mapsIdAnnotation = AssociationHelper.INSTANCE.locateMapsId(
				backingMember,
				attributeNature,
				container.getLocalBindingContext()
		);

		if ( this.mappedByAttributeName == null ) {
			// todo : not at all a fan of this mess...
			AssociationHelper.INSTANCE.processJoinColumnAnnotations(
					backingMember,
					joinColumnValues,
					getContext()
			);
			AssociationHelper.INSTANCE.processJoinTableAnnotations(
					backingMember,
					joinColumnValues,
					inverseJoinColumnValues,
					getContext()
			);
			this.joinTableAnnotation = AssociationHelper.INSTANCE.extractExplicitJoinTable(
					backingMember,
					getContext()
			);
			
			final AnnotationInstance inverseAnnotation = backingMember.getAnnotations().get( HibernateDotNames.INVERSE );
			isInverse = inverseAnnotation != null;
		}
		else {
			this.joinTableAnnotation = null;
			isInverse = true;
		}
		joinColumnValues.trimToSize();
		inverseJoinColumnValues.trimToSize();

		this.hasPrimaryKeyJoinColumn = backingMember.getAnnotations().containsKey( JPADotNames.PRIMARY_KEY_JOIN_COLUMN )
				|| backingMember.getAnnotations().containsKey( JPADotNames.PRIMARY_KEY_JOIN_COLUMNS );
	}

	public String getTargetTypeName() {
		return target;
	}

	@Override
	public FetchStyle getFetchStyle() {
		return fetchStyle;
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public String getMappedByAttributeName() {
		return mappedByAttributeName;
	}

	public void setMappedByAttributeName(String mappedByAttributeName) {
		this.mappedByAttributeName = mappedByAttributeName;
	}

	@Override
	public boolean isInverse() {
		return isInverse;
	}

	@Override
	public Set<CascadeType> getJpaCascadeTypes() {
		return jpaCascadeTypes;
	}

	@Override
	public Set<org.hibernate.annotations.CascadeType> getHibernateCascadeTypes() {
		return hibernateCascadeTypes;
	}

	@Override
	public boolean isOrphanRemoval() {
		return isOrphanRemoval;
	}

	@Override
	public boolean isIgnoreNotFound() {
		return ignoreNotFound;
	}

	@Override
	public boolean isUnWrapProxy() {
		return isUnWrapProxy;
	}

	public boolean hasPrimaryKeyJoinColumn() {
		return hasPrimaryKeyJoinColumn;
	}

	@Override
	public AttributeTypeResolver getHibernateTypeResolver() {
		if ( resolver == null ) {
			resolver = getDefaultHibernateTypeResolver();
		}
		return resolver;
	}

	private AttributeTypeResolver getDefaultHibernateTypeResolver() {
		return new CompositeAttributeTypeResolver(
				this,
				HibernateTypeResolver.createAttributeTypeResolver( this )
		);
	}

	@Override
	public boolean isOptional() {
		return isOptional;
	}

	@Override
	public boolean isInsertable() {
		// todo : this is configurable right?
		return true;
	}

	@Override
	public boolean isUpdatable() {
		// todo : this is configurable right?
		return true;
	}

	@Override
	public boolean isIncludeInOptimisticLocking() {
		if ( hasOptimisticLockAnnotation() ) {
			return super.isIncludeInOptimisticLocking();
		}
		else {
			// uh, saywhatnow?
			return isInsertable();
		}
	}

	@Override
	public PropertyGeneration getPropertyGeneration() {
		return PropertyGeneration.NEVER;
	}

	public AnnotationInstance getJoinTableAnnotation() {
		return joinTableAnnotation;
	}

	public ArrayList<Column> getJoinColumnValues() {
		return joinColumnValues;
	}

	public ArrayList<Column> getInverseJoinColumnValues() {
		return inverseJoinColumnValues;
	}

	public AnnotationInstance getMapsIdAnnotation() {
		return mapsIdAnnotation;
	}
}