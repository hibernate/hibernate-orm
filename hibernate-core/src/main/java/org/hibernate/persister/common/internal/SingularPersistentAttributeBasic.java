/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.persister.common.BasicValuedNavigable;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ConvertibleNavigable;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeBasic<O,J> extends AbstractSingularPersistentAttribute<O, J, BasicType<J>>
		implements ConvertibleNavigable<J>, BasicValuedNavigable<J> {
	private static final Logger log = Logger.getLogger( SingularPersistentAttributeBasic.class );

	private final Column boundColumn;

	private AttributeConverterDefinition attributeConverterInfo;

	public SingularPersistentAttributeBasic(
			ManagedTypeImplementor declaringType,
			String name,
			PropertyAccess propertyAccess,
			BasicType<J> ormType,
			Disposition disposition,
			AttributeConverterDefinition attributeConverterInfo,
			List<Column> columns) {
		super( declaringType, name, propertyAccess, ormType, disposition, true );

		assert columns.size() == 1;

		this.attributeConverterInfo = attributeConverterInfo;
		this.boundColumn = columns.get( 0 );
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public Column getBoundColumn() {
		return boundColumn;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( getBoundColumn() );
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeBasic(" + getContainer().asLoggableText() + '.' + getAttributeName() + ')';
	}

	@Override
	public AttributeConverterDefinition getAttributeConverter() {
		return attributeConverterInfo;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}
}
