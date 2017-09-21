/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Set;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassImpl<J>
		extends AbstractIdentifiableType<J>
		implements MappedSuperclassDescriptor<J> {
	public MappedSuperclassImpl(
			IdentifiableTypeMapping bootMapping,
			RuntimeModelCreationContext creationContext) {
		super( bootMapping, bootMapping.getJavaTypeDescriptor(), creationContext );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.MAPPED_SUPERCLASS;
	}

	@Override
	public void finishInstantiation(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeDescriptor<? super J> superType,
			IdentifiableTypeMapping bootMapping,
			RuntimeModelCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public void completeInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeDescriptor<? super J> superType,
			IdentifiableTypeMappingImplementor bootMapping,
			RuntimeModelCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception(  );
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
	public Type<?> getIdType() {
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
	public SqlSelectionGroup resolveSqlSelectionGroup(
			ColumnReferenceQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		// todo (6.0) : we'd have to know all subclasses to be able to generate selection-clause all columns we possibly need for all subtypes
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
