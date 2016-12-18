/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.type.spi.EntityType;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeEntity
		extends AbstractSingularAttribute<org.hibernate.type.spi.EntityType, EntityType> {
	private final SingularAttribute.Classification classification;
	private final Column[] columns;

	public SingularAttributeEntity(
			ManagedType declaringType,
			String name,
			SingularAttribute.Classification classification,
			org.hibernate.type.spi.EntityType ormType,
			EntityType sqmType,
			Column[] columns) {
		super( declaringType, name, ormType, sqmType );
		this.classification = classification;
		this.columns = columns;
	}

	@Override
	public SingularAttribute.Classification getAttributeTypeClassification() {
		return classification;
	}

	public Column[] getColumns() {
		return columns;
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
	public EntityType asManagedType() {
		return getBoundType();
	}
}
