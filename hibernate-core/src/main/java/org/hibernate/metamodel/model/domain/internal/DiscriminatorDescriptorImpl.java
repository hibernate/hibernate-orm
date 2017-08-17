/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.Member;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorMappings;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.produce.spi.SqlExpressionQualifier;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.internal.SqlSelectionGroupImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.descriptor.spi.ValueBinder;
import org.hibernate.type.descriptor.spi.ValueExtractor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorDescriptorImpl<O,J> implements DiscriminatorDescriptor<O,J> {
	public static final String NAVIGABLE_NAME = "{discriminator}";

	private final EntityHierarchy hierarchy;
	private final BasicType<J> basicType;
	private final Column column;

	private final NavigableRole navigableRole;

	public DiscriminatorDescriptorImpl(
			EntityHierarchy hierarchy,
			BasicValueMapping<J> valueMapping,
			RuntimeModelCreationContext creationContext) {
		this.hierarchy = hierarchy;

		this.basicType = valueMapping.resolveType();
		this.column = creationContext.getDatabaseObjectResolver().resolveColumn( valueMapping.getMappedColumn() );

		this.navigableRole = hierarchy.getRootEntityType().getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public ManagedTypeDescriptor<O> getContainer() {
		return hierarchy.getRootEntityType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		throw new org.hibernate.sql.NotYetImplementedException(  );
	}

	@Override
	public String getNavigableName() {
		return NAVIGABLE_NAME;
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
	public DiscriminatorMappings getDiscriminatorMappings() {
		// todo (6.0) : will probably need to collect these dynamically during "first phase" of runtime model creation
		throw new NotYetImplementedException(  );
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitDiscriminator( this );
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( column );
	}

	@Override
	public ValueBinder getValueBinder() {
		return getBasicType().getValueBinder();
	}

	@Override
	public ValueExtractor getValueExtractor() {
		return getBasicType().getValueExtractor();
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public String asLoggableText() {
		return getContainer().asLoggableText() + '.' + NAVIGABLE_NAME;
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
	public javax.persistence.metamodel.Type<J> getType() {
		return this;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.SINGULAR_ATTRIBUTE;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getBasicType().getJavaType();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public Member getJavaMember() {
		return null;
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
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicType<J> getBasicType() {
		return basicType;
	}

	@Override
	public QueryResult createQueryResult(
			Expression expression,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		final SqlSelectionGroup group = resolveSqlSelectionGroup(
				( (NavigableReference) expression ).getSqlExpressionQualifier(),
				creationContext
		);

		return new ScalarQueryResultImpl(
				resultVariable,
				group.getSqlSelections().get( 0 ),
				this
		);
	}

	private SqlSelection resolveSqlSelection(
			SqlExpressionQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		return resolutionContext.getSqlSelectionResolver().resolveSqlSelection(
				resolutionContext.getSqlSelectionResolver().resolveSqlExpression(
						qualifier,
						getBoundColumn()
				)
		);
	}

	@Override
	public SqlSelectionGroup resolveSqlSelectionGroup(
			SqlExpressionQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		return SqlSelectionGroupImpl.of( resolveSqlSelection( qualifier, resolutionContext ) );
	}
}
