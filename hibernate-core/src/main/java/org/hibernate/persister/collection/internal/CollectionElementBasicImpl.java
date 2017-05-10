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
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.persister.collection.spi.AbstractCollectionElement;
import org.hibernate.persister.collection.spi.CollectionElementBasic;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.Return;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class CollectionElementBasicImpl<J>
		extends AbstractCollectionElement<J,BasicType<J>>
		implements CollectionElementBasic<J> {
	private static final Logger log = Logger.getLogger( CollectionElementBasicImpl.class );

	private final AttributeConverterDefinition attributeConverter;

	public CollectionElementBasicImpl(
			CollectionPersister persister,
			Collection mappingBinding,
			BasicType<J> ormType,
			List<Column> columns) {
		super( persister, ormType, columns );

		final SimpleValue simpleElementValueMapping = (SimpleValue) mappingBinding.getElement();
		this.attributeConverter = simpleElementValueMapping.getAttributeConverterDescriptor();

		log.debugf(
				"AttributeConverter [%s] being injected for elements of the '%s' collection; was : %s",
				attributeConverter.getAttributeConverter(),
				getSource().getRole(),
				this.attributeConverter
		);
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
	public ElementClassification getClassification() {
		return ElementClassification.BASIC;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionElementBasic( this );
	}

	@Override
	public Return generateReturn(
			QueryResultCreationContext returnResolutionContext, TableGroup tableGroup) {
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
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( this );
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter interpreter) {
		interpreter.visitPluralAttributeIndex( this );
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return getOrmType().getJavaTypeDescriptor();
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return getOrmType().getSqlTypeDescriptor();
	}
}
