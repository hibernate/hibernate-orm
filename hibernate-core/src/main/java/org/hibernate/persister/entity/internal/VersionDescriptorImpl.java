/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.RootClass;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.VersionDescriptor;
import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.expression.domain.NavigablePath;
import org.hibernate.sql.ast.expression.domain.NavigableReferenceExpression;
import org.hibernate.sql.ast.expression.domain.SingularAttributeReferenceExpression;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.ast.from.TableSpace;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.SelectableBasicTypeImpl;
import org.hibernate.sql.convert.internal.FromClauseIndex;
import org.hibernate.sql.convert.internal.SqlAliasBaseManager;
import org.hibernate.sql.convert.results.spi.Fetch;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;
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

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitVersion( this );
	}

	@Override
	public TableGroup buildTableGroup(
			TableSpace tableSpace, SqlAliasBaseManager sqlAliasBaseManager, FromClauseIndex fromClauseIndex) {
		throw new NotYetImplementedException();
	}

	@Override
	public Return generateReturn(
			ReturnResolutionContext returnResolutionContext, TableGroup tableGroup) {
		return null;
	}

	@Override
	public Fetch generateFetch(
			ReturnResolutionContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent) {
		throw new UnsupportedOperationException();
	}

	private static class SelectableImpl implements Selectable, NavigableReferenceExpression {
		private final SingularAttributeReferenceExpression expressionDelegate;
		private final SelectableBasicTypeImpl selectableDelegate;
		private final NavigablePath navigablePath;

		public SelectableImpl(
				VersionDescriptorImpl versionDescriptor,
				ReturnResolutionContext returnResolutionContext,
				TableGroup tableGroup) {
			this.navigablePath = returnResolutionContext.currentNavigablePath().append( versionDescriptor.getNavigableName() );

			this.expressionDelegate = new SingularAttributeReferenceExpression(
					tableGroup,
					versionDescriptor,
					navigablePath
			);
			this.selectableDelegate = new SelectableBasicTypeImpl(
					this,
					getColumnBindings().get( 0 ),
					getType()
			);
		}

		@Override
		public BasicType getType() {
			return (BasicType) expressionDelegate.getType();
		}

		@Override
		public Selectable getSelectable() {
			return this;
		}

		@Override
		public void accept(SqlSelectAstToJdbcSelectConverter walker) {
			// todo (6.0) : do we need a separate "visitEntityIdentifier" method(s)?

			walker.visitSingularAttributeReference( expressionDelegate );
		}

		@Override
		public Expression getSelectedExpression() {
			return expressionDelegate;
		}

		@Override
		public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
			return selectableDelegate.toQueryReturn( returnResolutionContext, resultVariable );
		}

		@Override
		public Navigable getNavigable() {
			return expressionDelegate.getNavigable();
		}

		@Override
		public NavigablePath getNavigablePath() {
			return expressionDelegate.getNavigablePath();
		}

		@Override
		public List<ColumnBinding> getColumnBindings() {
			return expressionDelegate.getColumnBindings();
		}
	}

}
