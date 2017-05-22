/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.model.domain.spi.NavigableRole;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class RowIdDescriptorImpl implements RowIdDescriptor {
	private final EntityHierarchy hierarchy;
	// todo : really need to expose AbstractEntityPersister.rowIdName for this to work.
	//		for now we will just always assume a selection name of "ROW_ID"
	private final PhysicalColumn column;

	public RowIdDescriptorImpl(EntityHierarchy hierarchy) {
		this.hierarchy = hierarchy;
		column = new PhysicalColumn(
				hierarchy.getRootEntityType().getPrimaryTable(),
				"ROW_ID",
				Integer.MAX_VALUE
		);

	}

	@Override
	public Type getOrmType() {
		// what should this be?
		throw new NotYetImplementedException(  );
	}

	@Override
	public NavigableRole getNavigableRole() {
		// what should this be?
		throw new NotYetImplementedException(  );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		// what should this be?
		throw new NotYetImplementedException(  );
	}

	@Override
	public String getAttributeName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public Disposition getDisposition() {
		return Disposition.NORMAL;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( column );
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public String asLoggableText() {
		return NAVIGABLE_NAME;
	}

	@Override
	public boolean isId() {
		return false;
	}

	@Override
	public boolean isVersion() {
		return false;
	}

	@Override
	public boolean isOptional() {
		return false;
	}

	@Override
	public javax.persistence.metamodel.Type getType() {
		return this;
	}

	@Override
	public ManagedTypeImplementor getContainer() {
		return hierarchy.getRootEntityType();
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitRowId( this );
	}

	@Override
	public QueryResult generateReturn(
			QueryResultCreationContext returnResolutionContext, TableGroup tableGroup) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Fetch generateFetch(
			QueryResultCreationContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.SINGULAR_ATTRIBUTE;
	}

	@Override
	public Class getBindableJavaType() {
		return getOrmType().getJavaType();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	public boolean isCollection() {
		return false;
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
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public Member getJavaMember() {
		return null;
	}

	@Override
	public String getTypeName() {
		return getOrmType().getName();
	}
}
