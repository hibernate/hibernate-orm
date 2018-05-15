/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Set;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.spi.ManagedTypeMappingImplementor;
import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeImpl<J>
		extends AbstractIdentifiableType<J>
		implements MappedSuperclassTypeDescriptor<J> {
	private final EntityHierarchy hierarchy;

	@SuppressWarnings("unchecked")
	public MappedSuperclassTypeImpl(
			IdentifiableTypeMapping bootMapping,
			EntityHierarchy hierarchy,
			IdentifiableTypeDescriptor<? super J> superTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		super(
				bootMapping,
				superTypeDescriptor,
				(IdentifiableJavaDescriptor<J>) bootMapping.getJavaTypeMapping().getJavaTypeDescriptor(),
				creationContext
		);
		this.hierarchy = hierarchy;
	}

	@Override
	public EntityHierarchy getHierarchy() {
		return hierarchy;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.MAPPED_SUPERCLASS;
	}

	@Override
	public boolean finishInitialization(
			ManagedTypeMappingImplementor bootDescriptor,
			RuntimeModelCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		return new SubGraphImpl(
				this,
				true,
				getTypeConfiguration().getSessionFactory()
		);
	}

	@Override
	public <Y> SingularAttribute<? super J, Y> getId(Class<Y> type) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public <Y> SingularAttribute<J, Y> getDeclaredId(Class<Y> type) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public <Y> SingularAttribute<? super J, Y> getVersion(Class<Y> type) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public <Y> SingularAttribute<J, Y> getDeclaredVersion(Class<Y> type) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public boolean hasSingleIdAttribute() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public boolean hasVersionAttribute() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public Set<SingularAttribute<? super J, ?>> getIdClassAttributes() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public SimpleTypeDescriptor<?> getIdType() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public NavigableContainer getContainer() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public String asLoggableText() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public NavigableRole getNavigableRole() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public IdentifiableTypeDescriptor<? super J> getSupertype() {
		throw new NotYetImplementedFor6Exception(  );
	}
}
