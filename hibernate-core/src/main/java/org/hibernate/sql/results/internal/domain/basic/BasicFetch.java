/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.basic;

import java.util.function.Consumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.ConvertibleNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicFetch implements Fetch {
	private final FetchParent fetchParent;
	private final BasicValuedNavigable<?> navigable;
	private final FetchTiming fetchTiming;

	private final SqlSelection sqlSelection;

	private final NavigablePath path;

	public BasicFetch(
			FetchParent fetchParent,
			BasicValuedNavigable<?> navigable,
			FetchTiming fetchTiming,
			DomainResultCreationContext creationContext,
			DomainResultCreationState creationState) {
		this.fetchParent = fetchParent;
		this.navigable = navigable;
		this.fetchTiming = fetchTiming;

		SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();
		sqlSelection = sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						creationState.getColumnReferenceQualifierStack().getCurrent(),
						getFetchedNavigable().getBoundColumn()
				),
				getFetchedNavigable().getJavaTypeDescriptor(),
				creationContext.getSessionFactory().getTypeConfiguration()
		);

		this.path = fetchParent.getNavigablePath().append( navigable.getNavigableName() );
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public BasicValuedNavigable<?> getFetchedNavigable() {
		return navigable;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return path;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationContext creationContext,
			AssemblerCreationState creationState) {

		BasicValueConverter valueConverter = null;
		if ( getFetchedNavigable() instanceof ConvertibleNavigable ) {
			valueConverter = ( (ConvertibleNavigable) getFetchedNavigable() ).getValueConverter();
		}

		return new BasicResultAssembler(
				sqlSelection,
				valueConverter,
				getFetchedNavigable().getJavaTypeDescriptor()
		);
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getFetchedNavigable().getJavaTypeDescriptor();
	}
}
