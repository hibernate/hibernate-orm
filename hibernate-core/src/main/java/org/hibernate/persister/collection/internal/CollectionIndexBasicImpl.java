/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.List;
import javax.persistence.metamodel.Type;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.persister.collection.spi.AbstractCollectionIndex;
import org.hibernate.persister.collection.spi.CollectionIndexBasic;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexBasicImpl<J>
		extends AbstractCollectionIndex<J, BasicType<J>>
		implements CollectionIndexBasic<J> {
	private static final Logger log = Logger.getLogger( CollectionIndexBasicImpl.class );

	private final AttributeConverterDefinition attributeConverter;

	public CollectionIndexBasicImpl(
			CollectionPersister persister,
			IndexedCollection mappingBinding,
			BasicType<J> ormType,
			List<Column> columns) {
		super( persister, ormType, columns );

		final SimpleValue simpleValueMapping = (SimpleValue) mappingBinding.getIndex();
		this.attributeConverter = simpleValueMapping.getAttributeConverterDescriptor();
	}

	@Override
	public Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.BASIC;
	}

	@Override
	public AttributeConverterDefinition getAttributeConverter() {
		return attributeConverter;
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return null;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionIndexBasic( this );
	}

	@Override
	public Return generateReturn(
			QueryResultCreationContext returnResolutionContext,
			TableGroup tableGroup) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Fetch generateFetch(
			QueryResultCreationContext returnResolutionContext,
			TableGroup tableGroup,
			FetchParent fetchParent) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public IndexClassification getClassification() {
		return IndexClassification.BASIC;
	}
}
