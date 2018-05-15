/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal.entity;

import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.metamodel.model.relational.spi.DerivedColumn;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class RowIdDescriptorImpl<J> implements RowIdDescriptor<J> {
	private final EntityHierarchy hierarchy;
	// todo : really need to expose AbstractEntityPersister.rowIdName for this to work.
	//		for now we will just always assume a selection name of "ROW_ID"
	private final DerivedColumn column;

	public RowIdDescriptorImpl(
			EntityHierarchy hierarchy,
			RuntimeModelCreationContext creationContext) {
		this.hierarchy = hierarchy;
		column = new DerivedColumn(
				hierarchy.getRootEntityType().getPrimaryTable(),
				"ROW_ID",
				creationContext.getTypeConfiguration()
						.getSqlTypeDescriptorRegistry()
						.getDescriptor( Types.INTEGER ),
				creationContext.getTypeConfiguration()
		);
	}

	@Override
	public NavigableRole getNavigableRole() {
		// what should this be?
		throw new NotYetImplementedException(  );
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		// what should this be?
		throw new NotYetImplementedException(  );
	}

	@Override
	public String asLoggableText() {
		return NAVIGABLE_NAME;
	}

	@Override
	public ManagedTypeDescriptor getContainer() {
		return hierarchy.getRootEntityType();
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitRowIdDescriptor( this );
	}

	@Override
	public String getNavigableName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public DomainResult createDomainResult(
			NavigableReference navigableReference,
			String resultVariable,
			DomainResultCreationState creationState, DomainResultCreationContext creationContext) {
		// todo (6.0) : but like with discriminator, etc we *could* if we wanted to
		//		exposed as a "virtual attribute" such as `select p.{type} from Person p`
		throw new HibernateException( "Selection of ROW_ID from domain query is not supported" );
	}

	@Override
	public DerivedColumn getBoundColumn() {
		return column;
	}

	@Override
	public BasicType<J> getBasicType() {
		return null;
	}
}
