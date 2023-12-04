/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.MultipleAttributeNaturesException;
import org.hibernate.boot.models.categorize.ModelCategorizationLogging;
import org.hibernate.boot.models.categorize.spi.AllMemberConsumer;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Models metadata about a JPA {@linkplain jakarta.persistence.metamodel.ManagedType managed-type}.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class AbstractManagedTypeMetadata implements ManagedTypeMetadata {
	private final ClassDetails classDetails;
	private final ModelCategorizationContext modelContext;

	private final AttributePath attributePathBase;
	private final AttributeRole attributeRoleBase;

	/**
	 * This form is intended for construction of the root of an entity hierarchy
	 * and its mapped-superclasses
	 */
	public AbstractManagedTypeMetadata(ClassDetails classDetails, ModelCategorizationContext modelContext) {
		this.classDetails = classDetails;
		this.modelContext = modelContext;
		this.attributeRoleBase = new AttributeRole( classDetails.getName() );
		this.attributePathBase = new AttributePath();
	}

	/**
	 * This form is used to create Embedded references
	 *
	 * @param classDetails The Embeddable descriptor
	 * @param attributeRoleBase The base for the roles of attributes created *from* here
	 * @param attributePathBase The base for the paths of attributes created *from* here
	 */
	public AbstractManagedTypeMetadata(
			ClassDetails classDetails,
			AttributeRole attributeRoleBase,
			AttributePath attributePathBase,
			ModelCategorizationContext modelContext) {
		this.classDetails = classDetails;
		this.modelContext = modelContext;
		this.attributeRoleBase = attributeRoleBase;
		this.attributePathBase = attributePathBase;
	}

	public ClassDetails getClassDetails() {
		return classDetails;
	}

	public ModelCategorizationContext getModelContext() {
		return modelContext;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		AbstractManagedTypeMetadata that = (AbstractManagedTypeMetadata) o;
		return Objects.equals( classDetails.getName(), that.classDetails.getName() );
	}

	@Override
	public int hashCode() {
		return Objects.hash( classDetails );
	}

	@Override
	public String toString() {
		return "ManagedTypeMetadata(" + classDetails.getName() + ")";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// attribute handling

	protected abstract List<AttributeMetadata> attributeList();

	@Override
	public int getNumberOfAttributes() {
		return attributeList().size();
	}

	@Override
	public Collection<AttributeMetadata> getAttributes() {
		return attributeList();
	}

	@Override
	public AttributeMetadata findAttribute(String name) {
		final List<AttributeMetadata> attributeList = attributeList();
		for ( int i = 0; i < attributeList.size(); i++ ) {
			final AttributeMetadata attribute = attributeList.get( i );
			if ( attribute.getName().equals( name ) ) {
				return attribute;
			}
		}
		return null;
	}

	@Override
	public void forEachAttribute(IndexedConsumer<AttributeMetadata> consumer) {
		for ( int i = 0; i < attributeList().size(); i++ ) {
			consumer.accept( i, attributeList().get( i ) );
		}
	}

	protected List<AttributeMetadata> resolveAttributes(AllMemberConsumer memberConsumer) {
		final List<MemberDetails> backingMembers = getModelContext()
				.getPersistentAttributeMemberResolver()
				.resolveAttributesMembers( classDetails, getAccessType(), memberConsumer, modelContext );

		final List<AttributeMetadata> attributeList = arrayList( backingMembers.size() );

		for ( MemberDetails backingMember : backingMembers ) {
			final AttributeMetadata attribute = new AttributeMetadataImpl(
					backingMember.resolveAttributeName(),
					determineAttributeNature( backingMember ),
					backingMember
			);
			attributeList.add( attribute );
		}

		return attributeList;
	}

	/**
	 * Determine the attribute's nature - is it a basic mapping, an embeddable, ...?
	 *
	 * Also performs some simple validation around multiple natures being indicated
	 */
	private AttributeMetadata.AttributeNature determineAttributeNature(MemberDetails backingMember) {
		final EnumSet<AttributeMetadata.AttributeNature> natures = EnumSet.noneOf( AttributeMetadata.AttributeNature.class );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// first, look for explicit nature annotations

		final AnnotationUsage<Any> any = backingMember.getAnnotationUsage( HibernateAnnotations.ANY );
		final AnnotationUsage<Basic> basic = backingMember.getAnnotationUsage( JpaAnnotations.BASIC );
		final AnnotationUsage<ElementCollection> elementCollection = backingMember.getAnnotationUsage( JpaAnnotations.ELEMENT_COLLECTION );
		final AnnotationUsage<Embedded> embedded = backingMember.getAnnotationUsage( JpaAnnotations.EMBEDDED );
		final AnnotationUsage<EmbeddedId> embeddedId = backingMember.getAnnotationUsage( JpaAnnotations.EMBEDDED_ID );
		final AnnotationUsage<ManyToAny> manyToAny = backingMember.getAnnotationUsage( HibernateAnnotations.MANY_TO_ANY );
		final AnnotationUsage<ManyToMany> manyToMany = backingMember.getAnnotationUsage( JpaAnnotations.MANY_TO_MANY );
		final AnnotationUsage<ManyToOne> manyToOne = backingMember.getAnnotationUsage( JpaAnnotations.MANY_TO_ONE );
		final AnnotationUsage<OneToMany> oneToMany = backingMember.getAnnotationUsage( JpaAnnotations.ONE_TO_MANY );
		final AnnotationUsage<OneToOne> oneToOne = backingMember.getAnnotationUsage( JpaAnnotations.ONE_TO_ONE );

		if ( basic != null ) {
			natures.add( AttributeMetadata.AttributeNature.BASIC );
		}

		if ( embedded != null
				|| embeddedId != null
				|| ( backingMember.getType() != null && backingMember.getType().getAnnotationUsage( JpaAnnotations.EMBEDDABLE ) != null ) ) {
			natures.add( AttributeMetadata.AttributeNature.EMBEDDED );
		}

		if ( any != null ) {
			natures.add( AttributeMetadata.AttributeNature.ANY );
		}

		if ( oneToOne != null
				|| manyToOne != null ) {
			natures.add( AttributeMetadata.AttributeNature.TO_ONE );
		}

		final boolean plural = oneToMany != null
				|| manyToMany != null
				|| elementCollection != null
				|| manyToAny != null;
		if ( plural ) {
			natures.add( AttributeMetadata.AttributeNature.PLURAL );
		}

		// look at annotations that imply a nature
		//		NOTE : these could apply to the element or index of collection, so
		//		only do these if it is not a collection

		if ( !plural ) {
			// first implicit basic nature
			if ( backingMember.getAnnotationUsage( JpaAnnotations.TEMPORAL ) != null
					|| backingMember.getAnnotationUsage( JpaAnnotations.LOB ) != null
					|| backingMember.getAnnotationUsage( JpaAnnotations.ENUMERATED ) != null
					|| backingMember.getAnnotationUsage( JpaAnnotations.CONVERT ) != null
					|| backingMember.getAnnotationUsage( JpaAnnotations.VERSION ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.GENERATED ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.NATIONALIZED ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.TZ_COLUMN ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.TZ_STORAGE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.TYPE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.TENANT_ID ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.JAVA_TYPE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.JDBC_TYPE_CODE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.JDBC_TYPE ) != null ) {
				natures.add( AttributeMetadata.AttributeNature.BASIC );
			}

			// then embedded
			if ( backingMember.getAnnotationUsage( HibernateAnnotations.EMBEDDABLE_INSTANTIATOR ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.COMPOSITE_TYPE ) != null ) {
				natures.add( AttributeMetadata.AttributeNature.EMBEDDED );
			}

			// and any
			if ( backingMember.getAnnotationUsage( HibernateAnnotations.ANY_DISCRIMINATOR ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_DISCRIMINATOR_VALUE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_DISCRIMINATOR_VALUES ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_KEY_JAVA_TYPE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_KEY_JAVA_CLASS ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_KEY_JDBC_TYPE ) != null
					|| backingMember.getAnnotationUsage( HibernateAnnotations.ANY_KEY_JDBC_TYPE_CODE ) != null ) {
				natures.add( AttributeMetadata.AttributeNature.ANY );
			}
		}

		int size = natures.size();
		switch ( size ) {
			case 0: {
				ModelCategorizationLogging.MODEL_CATEGORIZATION_LOGGER.debugf(
						"Implicitly interpreting attribute `%s` as BASIC",
						backingMember.resolveAttributeName()
				);
				return AttributeMetadata.AttributeNature.BASIC;
			}
			case 1: {
				return natures.iterator().next();
			}
			default: {
				throw new MultipleAttributeNaturesException( backingMember.resolveAttributeName(), natures );
			}
		}
	}

//	@Override
//	public <A extends Annotation> List<AnnotationUsage<A>> findAnnotations(AnnotationDescriptor<A> type) {
//		return classDetails.getAnnotations( type );
//	}
//
//	@Override
//	public <A extends Annotation> void forEachAnnotation(AnnotationDescriptor<A> type, Consumer<AnnotationUsage<A>> consumer) {
//		classDetails.forEachAnnotation( type, consumer );
//	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Stuff affecting attributes built from this managed type.

	public boolean canAttributesBeInsertable() {
		return true;
	}

	public boolean canAttributesBeUpdatable() {
		return true;
	}

	public NaturalIdMutability getContainerNaturalIdMutability() {
		return NaturalIdMutability.NOT_NATURAL_ID;
	}
}
