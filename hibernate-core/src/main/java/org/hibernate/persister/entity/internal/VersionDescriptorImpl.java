/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.mapping.RootClass;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.VersionDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class VersionDescriptorImpl<O,J> extends AbstractSingularPersistentAttribute<O,J,BasicType<J>> implements VersionDescriptor<O,J> {
	private final Column column;
	private final String unsavedValue;

	public VersionDescriptorImpl(
			EntityHierarchy hierarchy,
			RootClass rootEntityBinding,
			Column column,
			String name,
			BasicType<J> ormType,
			boolean nullable,
			String unsavedValue) {
		super(
				hierarchy.getRootEntityPersister(),
				name,
				PersisterHelper.resolvePropertyAccess( hierarchy.getRootEntityPersister(), rootEntityBinding.getVersion() ),
				ormType,
				Disposition.NORMAL,
				nullable
		);
		this.column = column;
		this.unsavedValue = unsavedValue;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public String getUnsavedValue() {
		return unsavedValue;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( column );
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return getSource().asLoggableText() + '.' + getNavigableName();
	}
}
