/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.entity.internal;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.PhysicalColumn;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.RowIdDescriptor;
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
				hierarchy.getRootEntityPersister().getRootTable(),
				"ROW_ID",
				Integer.MAX_VALUE
		);

	}

	@Override
	public Type getOrmType() {
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
	public ManagedTypeImplementor getSource() {
		return hierarchy.getRootEntityPersister();
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
