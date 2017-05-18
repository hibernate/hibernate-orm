/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.queryable.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.persister.common.spi.NavigableRole;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableContainer;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.queryable.spi.PolymorphicEntityValuedExpressableType;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Hibernate's standard PolymorphicEntityValuedExpressableType impl.
 *
 * @author Steve Ebersole
 */
public class PolymorphicEntityValuedExpressableTypeImpl<T> implements PolymorphicEntityValuedExpressableType<T> {
	private final JavaTypeDescriptor<T> javaType;
	private final Set<EntityPersister<?>> implementors;
	private final NavigableRole navigableRole;

	public PolymorphicEntityValuedExpressableTypeImpl(JavaTypeDescriptor<T> javaType, Set<EntityPersister<?>> implementors) {
		this.javaType = javaType;
		this.implementors = implementors;
		this.navigableRole = new NavigableRole( asLoggableText() );
	}

	@Override
	public Set<EntityPersister<?>> getImplementors() {
		return new HashSet<>( implementors );
	}

	@Override
	public NavigableContainer getContainer() {
		return null;
	}

	@Override
	public Class<T> getJavaType() {
		return javaType.getJavaType();
	}

	@Override
	public EntityPersister<T> getEntityPersister() {
		// todo (6.0) - throw an exception?
		return null;
	}

	@Override
	public String getEntityName() {
		return getJavaType().getName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityName();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String asLoggableText() {
		return "PolymorphicEntityValuedNavigable( " + getEntityName() + ")";
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// todo (6.0) : decide what to do for these.
	//		they are examples of some of the unwanted leakages mentioned on
	//		Navigable and NavigableSource

	@Override
	@SuppressWarnings("unchecked")
	public <N> Navigable<N> findNavigable(String navigableName) {
		// only return navigables that all of the implementors define
		Navigable navigable = null;
		for ( EntityPersister implementor : implementors ) {
			final Navigable current = implementor.findNavigable( navigableName );
			if ( current == null ) {
				return null;
			}
			if ( navigable == null ) {
				navigable = current;
			}
		}

		return navigable;
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		// todo (6.0) : what is the proper response here?
		//		for now, just return all navigables.  this particular
		//		feature is beyond the JPA spec, so adherence to the spec
		//		here is not important.
		return findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return javaType;
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		throw new HibernateException( "Cannot create Selection from polymorphic entity reference" );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		throw new HibernateException( "Cannot create QueryResult from polymorphic entity reference" );
	}
}
