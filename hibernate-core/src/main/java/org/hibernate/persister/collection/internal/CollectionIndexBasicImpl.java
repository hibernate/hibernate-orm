/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.List;
import java.util.Optional;
import javax.persistence.metamodel.Type;

import org.hibernate.persister.collection.spi.AbstractCollectionIndex;
import org.hibernate.persister.collection.spi.CollectionIndexBasic;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexBasicImpl
		extends AbstractCollectionIndex<BasicType>
		implements CollectionIndexBasic {
	private static final Logger log = Logger.getLogger( CollectionIndexBasicImpl.class );

	private AttributeConverterDefinition attributeConverter;

	public CollectionIndexBasicImpl(
			CollectionPersister persister,
			BasicType ormType,
			List<Column> columns) {
		super( persister, ormType, columns );
	}

	@Override
	public Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.BASIC;
	}

	@Override
	public void injectAttributeConverter(AttributeConverterDefinition converter) {
		log.debugf(
				"AttributeConverter [%s] being injected for indexes of the '%s' collection; was : %s",
				converter,
				getSource().getRole(),
				this.attributeConverter
		);
		this.attributeConverter = converter;
	}

	@Override
	public Optional<AttributeConverterDefinition> getAttributeConverter() {
		return Optional.ofNullable( attributeConverter );
	}
}
