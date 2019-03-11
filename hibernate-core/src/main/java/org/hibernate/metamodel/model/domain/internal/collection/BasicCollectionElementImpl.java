/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.function.BiConsumer;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.BasicCollectionElement;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SimpleTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class BasicCollectionElementImpl<J>
		extends AbstractCollectionElement<J>
		implements BasicCollectionElement<J>, ConvertibleNavigable<J> {
	private static final Logger log = Logger.getLogger( BasicCollectionElementImpl.class );

	private final Column column;
	private final BasicValueMapper<J> valueMapper;
	private final boolean nullable;

	@SuppressWarnings("unchecked")
	public BasicCollectionElementImpl(
			PersistentCollectionDescriptor descriptor,
			Collection bootCollectionMapping,
			RuntimeModelCreationContext creationContext) {
		super( descriptor );

		final BasicValueMapping simpleElementValueMapping = (BasicValueMapping) bootCollectionMapping.getElement();

		this.column = creationContext.getDatabaseObjectResolver().resolveColumn( simpleElementValueMapping.getMappedColumn() );

		this.valueMapper = ( (BasicValueMapping) bootCollectionMapping.getElement() ).getResolution().getValueMapper();

		if ( valueMapper.getValueConverter() != null ) {
			log.debugf(
					"BasicValueConverter [%s] being applied for basic collection elements : %s",
					valueMapper.getValueConverter(),
					getNavigableRole()
			);
		}

		this.nullable = bootCollectionMapping.getElement().isNullable();
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return valueMapper.getDomainJavaDescriptor();
	}

	@Override
	public BasicValueConverter getValueConverter() {
		return valueMapper.getValueConverter();
	}

	@Override
	public ElementClassification getClassification() {
		return ElementClassification.BASIC;
	}

	@Override
	public SimpleTypeDescriptor getDomainTypeDescriptor() {
		return this;
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		action.accept( column.getExpressableType(), column );
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return valueMapper;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return valueMapper.getSqlExpressableType();
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		// nothing to do
	}

	@Override
	public boolean hasNotNullColumns() {
		return !nullable;
	}

	@Override
	public boolean isMutable() {
		return getJavaTypeDescriptor().getMutabilityPlan().isMutable();
	}
}
