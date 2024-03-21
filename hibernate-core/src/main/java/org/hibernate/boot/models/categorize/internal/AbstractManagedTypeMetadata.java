/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.model.source.spi.AttributeRole;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.models.categorize.spi.AllMemberConsumer;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ModelCategorizationContext;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

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
				.resolveAttributesMembers( classDetails, getClassLevelAccessType(), memberConsumer );

		final List<AttributeMetadata> attributeList = arrayList( backingMembers.size() );

		for ( MemberDetails backingMember : backingMembers ) {
			final AttributeMetadata attribute = new AttributeMetadataImpl(
					backingMember.resolveAttributeName(),
					CategorizationHelper.determineAttributeNature( classDetails, backingMember ),
					backingMember
			);
			attributeList.add( attribute );
		}

		return attributeList;
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
