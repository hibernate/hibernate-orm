/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.queryable.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableSource;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.queryable.spi.PolymorphicEntityValuedExpressableType;
import org.hibernate.sql.tree.from.TableGroup;
import org.hibernate.sql.tree.from.TableSpace;
import org.hibernate.sql.convert.internal.FromClauseIndex;
import org.hibernate.sql.convert.internal.SqlAliasBaseManager;
import org.hibernate.sql.convert.results.spi.Fetch;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;

/**
 * Hibernate's standard PolymorphicEntityValuedExpressableType impl.
 *
 * @author Steve Ebersole
 */
public class PolymorphicEntityValuedExpressableTypeImpl<T> implements PolymorphicEntityValuedExpressableType<T> {
	private final Class<T> javaType;
	private final Set<EntityPersister<?>> implementors;
	private final NavigableRole navigableRole;

	public PolymorphicEntityValuedExpressableTypeImpl(Class<T> javaType, Set<EntityPersister<?>> implementors) {
		this.javaType = javaType;
		this.implementors = implementors;
		this.navigableRole = new NavigableRole( asLoggableText() );
	}

	@Override
	public Set<EntityPersister<?>> getImplementors() {
		return new HashSet<>( implementors );
	}

	@Override
	public NavigableSource getSource() {
		return null;
	}

	@Override
	public Class<T> getJavaType() {
		return javaType;
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
	public String getTypeName() {
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
	public List<JoinColumnMapping> resolveJoinColumnMappings(PersistentAttribute persistentAttribute) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public TableGroup buildTableGroup(
			TableSpace tableSpace, SqlAliasBaseManager sqlAliasBaseManager, FromClauseIndex fromClauseIndex) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Return generateReturn(
			ReturnResolutionContext returnResolutionContext, TableGroup tableGroup) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Fetch generateFetch(
			ReturnResolutionContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent) {
		throw new NotYetImplementedException(  );
	}
}
