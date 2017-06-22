/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.metamodel.spi.EmbeddedValueExpressableType;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * Mapping for an embeddable.
 * <P/>
 * NOTE - the name is here is Embedded* as opposed to Embeddable* because it
 * really represents the specific usage of the embeddable, which is `@Embedded`
 * <p/>
 * NOTE2 - this extends InheritanceCapable even though we currently do not support that, but we
 * know it is something we want to support asap.
 *
 * @author Steve Ebersole
 */
public interface EmbeddedTypeDescriptor<T>
		extends InheritanceCapable<T>, EmbeddedContainer<T>, EmbeddedValueExpressableType<T>, EmbeddableType<T>,
		EmbeddedValuedNavigable<T> {

	Class[] STANDARD_CTOR_SIGNATURE = new Class[] {
			Component.class,
			EmbeddedContainer.class,
			String.class,
			RuntimeModelCreationContext.class
	};

	/**
	 * Called after all managed types in the persistence unit have been
	 * instantiated.  Some will have been at least partially populated.
	 * <p/>
	 * At this point implementors ought to be able to locate other
	 * managed types.
	 *
	 * @param embeddedValueMapping The source boot-time mapping descriptor
	 * @param creationContext Access to services needed while finishing the instantiation
	 */
	void finishInstantiation(
			EmbeddedValueMapping embeddedValueMapping,
			RuntimeModelCreationContext creationContext);

	/**
	 * Called after all managed types in the persistence unit have been
	 * fully instantiated (all their `#finishInstantiation` calls have completed).
	 * <p/>
	 * At this point implementors ought to be able to rely on the complete Navigable structure to have
	 * at least been "stitched" together and most information has been populated (there are a few
	 * exceptions, which we ought to list in a Navigable "design doc").
	 *
	 * todo (6.0) : Create Navigable-design-doc either here in repo or as wiki
	 *
	 * @param embeddedValueMapping The source boot-time mapping descriptor
	 * @param creationContext Access to services needed while finishing the instantiation
	 */
	void completeInitialization(
			EmbeddedValueMapping embeddedValueMapping,
			RuntimeModelCreationContext creationContext);

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
}
