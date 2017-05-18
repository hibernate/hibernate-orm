/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.metamodel.model.domain.internal.PersisterHelper;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifiableType<T> extends AbstractManagedType<T> implements IdentifiableTypeImplementor<T> {
	private EntityHierarchy entityHierarchy;
	private IdentifiableTypeImplementor<? super T> superclassType;

	public AbstractIdentifiableType(IdentifiableJavaDescriptor<T> javaTypeDescriptor) {
		super( javaTypeDescriptor );
	}

	@Override
	public IdentifiableJavaDescriptor<T> getJavaTypeDescriptor() {
		return (IdentifiableJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@SuppressWarnings("unchecked")
	public IdentifiableTypeImplementor<? super T> getSuperclassType() {
		return superclassType;
	}

	@Override
	public EntityHierarchy getHierarchy() {
		return entityHierarchy;
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		getHierarchy().getIdentifierDescriptor().visitNavigable( visitor );
		super.visitDeclaredNavigables( visitor );
	}

	public void finishInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeImplementor<? super T> superType,
			IdentifiableTypeMapping mappingDescriptor,
			RuntimeModelCreationContext creationContext) {
		this.entityHierarchy = entityHierarchy;
		this.superclassType = superType;

		for ( PersistentAttributeMapping attributeMapping : mappingDescriptor.getDeclaredPersistentAttributes() ) {

			// todo : Columns
			final List<Column> columns = Collections.emptyList();

			final PersistentAttribute persistentAttribute = PersisterHelper.INSTANCE.buildAttribute(
					creationContext,
					this,
					attributeMapping,
					columns
			);
			addAttribute( persistentAttribute );
		}
	}
}
