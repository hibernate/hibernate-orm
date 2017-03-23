/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.mapping.IdentifiableTypeMapping;
import org.hibernate.mapping.Property;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractManagedType;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.IdentifiableTypeImplementor;
import org.hibernate.persister.spi.PersisterCreationContext;
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

	@Override
	public void finishInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeImplementor<? super T> superType,
			IdentifiableTypeMapping mappingDescriptor,
			PersisterCreationContext creationContext) {
		this.entityHierarchy = entityHierarchy;
		this.superclassType = superType;

		for ( Property property : mappingDescriptor.getDeclaredProperties() ) {

			// todo : Columns
			final List<Column> columns = Collections.emptyList();

			final PersistentAttribute persistentAttribute = PersisterHelper.INSTANCE.buildAttribute(
					creationContext,
					this,
					property,
					columns
			);
			addAttribute( persistentAttribute );
		}
	}
}
