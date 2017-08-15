/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.sql.ast.produce.metamodel.spi.PolymorphicEntityValuedExpressableType;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * Hibernate's standard PolymorphicEntityValuedExpressableType impl.
 *
 * @author Steve Ebersole
 */
public class PolymorphicEntityValuedExpressableTypeImpl<T> implements PolymorphicEntityValuedExpressableType<T> {
	private final EntityJavaDescriptor<T> javaType;
	private final Set<EntityDescriptor<?>> implementors;
	private final NavigableRole navigableRole;

	public PolymorphicEntityValuedExpressableTypeImpl(EntityJavaDescriptor<T> javaType, Set<EntityDescriptor<?>> implementors) {
		this.javaType = javaType;
		this.implementors = implementors;
		this.navigableRole = new NavigableRole( asLoggableText() );
	}

	@Override
	public Set<EntityDescriptor<?>> getImplementors() {
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
	public EntityDescriptor<T> getEntityDescriptor() {
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
		for ( EntityDescriptor implementor : implementors ) {
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
	public List<Navigable> getNavigables() {
		return null;
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return null;
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
	public EntityJavaDescriptor<T> getJavaTypeDescriptor() {
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
			SqlExpressionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		throw new HibernateException( "Cannot create QueryResult from polymorphic entity reference" );
	}
}
