/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.List;
import java.util.Optional;

import javax.persistence.AttributeConverter;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.AttributeContainer;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ConvertibleDomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeBasic extends AbstractSingularAttribute<BasicType> implements ConvertibleDomainReference {
	private final List<Column> columns;
	private final AttributeConverter attributeConverter;

	public SingularAttributeBasic(
			AttributeContainer declaringType,
			String name,
			BasicType ormType,
			AttributeConverter attributeConverter,
			List<Column> columns) {
		super( declaringType, name, ormType, true );
		this.attributeConverter = attributeConverter;
		this.columns = columns;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeBasic(" + getLeftHandSide().asLoggableText() + '.' + getAttributeName() + ')';
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}

	@Override
	public Optional<AttributeConverter> getAttributeConverter() {
		return Optional.of( attributeConverter );
	}
}
