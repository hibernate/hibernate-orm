/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * Mapping for an embeddable.
 * <P/>
 * NOTE - the name is here is Embedded* as opposed to Embeddable* because it
 * really represents the specific usage of the embeddable, which is `@Embedded`
 * <p/>
 * NOTE2 - this extends InheritanceCapable even though we currently do not support that, but we
 * know it is something we want to support at some point.
 *
 * @author Steve Ebersole
 */
public interface EmbeddedTypeDescriptor<T>
		extends InheritanceCapable<T>, EmbeddedContainer<T>, EmbeddableType<T>,	EmbeddedValuedNavigable<T> {

	Class[] STANDARD_CTOR_SIGNATURE = new Class[] {
			EmbeddedValueMappingImplementor.class,
			EmbeddedContainer.class,
			EmbeddedTypeDescriptor.class,
			String.class,
			SingularPersistentAttribute.Disposition.class,
			RuntimeModelCreationContext.class
	};

	default String getRoleName() {
		return getNavigableRole().getFullPath();
	}

	List<Column> collectColumns();

	@Override
	EmbeddableJavaDescriptor<T> getJavaTypeDescriptor();

	@Override
	EmbeddedContainer<?> getContainer();

	@Override
	@SuppressWarnings("unchecked")
	default Class<T> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	default boolean canCompositeContainCollections() {
		return getContainer().canCompositeContainCollections();
	}

	@Override
	default String asLoggableText() {
		return "EmbeddableMapper(" + getJavaType() + " [" + getRoleName() + "])";
	}

	@Override
	default int getNumberOfJdbcParametersForRestriction() {
		return getEmbeddedDescriptor().getNumberOfJdbcParametersForRestriction();
	}


	@Override
	default boolean isSubclassTypeName(String name) {
		return false;
	}




	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) : everything below relates to the "attribute position" discussion


	/**
	 * Get the nullability of the properties of this class
	 */
	boolean[] getPropertyNullability();

	/**
	 * Get the cascade style of a particular property
	 */
	CascadeStyle getCascadeStyle(int i);


}
