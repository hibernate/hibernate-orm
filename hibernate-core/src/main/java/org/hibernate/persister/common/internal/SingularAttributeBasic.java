/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.List;
import java.util.Optional;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ConvertibleNavigable;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sqm.domain.SqmSingularAttribute;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeBasic<O,J> extends AbstractSingularAttribute<O, J, BasicType<J>> implements ConvertibleNavigable<J> {
	private static final Logger log = Logger.getLogger( SingularAttributeBasic.class );

	private final List<Column> columns;

	private AttributeConverterDefinition attributeConverterInfo;

	public SingularAttributeBasic(
			ManagedTypeImplementor declaringType,
			String name,
			PropertyAccess propertyAccess,
			BasicType<J> ormType,
			Disposition disposition,
			AttributeConverterDefinition attributeConverterInfo,
			List<Column> columns) {
		super( declaringType, name, propertyAccess, ormType, disposition, true );
		this.attributeConverterInfo = attributeConverterInfo;
		this.columns = columns;
	}

	@Override
	public SqmSingularAttribute.SingularAttributeClassification getAttributeTypeClassification() {
		return SqmSingularAttribute.SingularAttributeClassification.BASIC;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeBasic(" + getSource().asLoggableText() + '.' + getAttributeName() + ')';
	}

	@Override
	public void injectAttributeConverter(AttributeConverterDefinition  attributeConverterInfo) {
		log.debugf(
				"AttributeConverter [%s] being injected for singular attribute '%s.%s' collection; was : %s",
				attributeConverterInfo,
				getSource().asLoggableText(),
				getName(),
				this.attributeConverterInfo
		);
		this.attributeConverterInfo = attributeConverterInfo;
	}

	@Override
	public Optional<AttributeConverterDefinition > getAttributeConverter() {
		return Optional.ofNullable( attributeConverterInfo );
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
