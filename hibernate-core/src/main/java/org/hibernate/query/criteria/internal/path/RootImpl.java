/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.path;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.CriteriaSubqueryImpl;
import org.hibernate.query.criteria.internal.FromImplementor;
import org.hibernate.query.criteria.internal.PathSource;
import org.hibernate.query.criteria.internal.compile.RenderingContext;

/**
 * Hibernate implementation of the JPA {@link Root} contract
 *
 * @author Steve Ebersole
 */
public class RootImpl<X> extends AbstractFromImpl<X,X> implements Root<X>, Serializable {
	private final EntityType<X> entityType;
	private final boolean allowJoins;
	private final Set<TreatedRoot<? extends X>> treats = new LinkedHashSet<>();

	public RootImpl(CriteriaBuilderImpl criteriaBuilder, EntityType<X> entityType) {
		this( criteriaBuilder, entityType, true );
	}

	public RootImpl(CriteriaBuilderImpl criteriaBuilder, EntityType<X> entityType, boolean allowJoins) {
		super( criteriaBuilder, entityType.getJavaType() );
		this.entityType = entityType;
		this.allowJoins = allowJoins;
	}

	public EntityType<X> getEntityType() {
		return entityType;
	}

	public EntityType<X> getModel() {
		return getEntityType();
	}

	@Override
	protected FromImplementor<X, X> createCorrelationDelegate() {
		return new RootImpl<X>( criteriaBuilder(), getEntityType() );
	}

	@Override
	public RootImpl<X> correlateTo(CriteriaSubqueryImpl subquery) {
		return (RootImpl<X>) super.correlateTo( subquery );
	}
	
	@Override
	protected boolean canBeJoinSource() {
		return allowJoins;
	}

	@Override
	@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
	protected RuntimeException illegalJoin() {
		return allowJoins ? super.illegalJoin() : new IllegalArgumentException( "UPDATE/DELETE criteria queries cannot define joins" );
	}

	@Override
	@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
	protected RuntimeException illegalFetch() {
		return allowJoins ? super.illegalFetch() : new IllegalArgumentException( "UPDATE/DELETE criteria queries cannot define fetches" );
	}

	public String renderTableExpression(RenderingContext renderingContext) {
		prepareAlias( renderingContext );
		return getModel().getName() + " as " + getAlias();
	}

	@Override
	public String getPathIdentifier() {
		return getAlias();
	}

	@Override
	public String render(RenderingContext renderingContext) {
		prepareAlias( renderingContext );
		return getAlias();
	}
	
	public Set<TreatedRoot<? extends X>> getTreats() {
		return treats;
	}

	@Override
	public <T extends X> RootImpl<T> treatAs(Class<T> treatAsType) {
		TreatedRoot<T> treatedRoot = new TreatedRoot<T>( this, treatAsType );
		treats.add(treatedRoot);
		return treatedRoot;
	}

	public static class TreatedRoot<T> extends RootImpl<T> {
		private final RootImpl<? super T> original;
		private final Class<T> treatAsType;

		public TreatedRoot(RootImpl<? super T> original, Class<T> treatAsType) {
			super(
					original.criteriaBuilder(),
					original.criteriaBuilder().getEntityManagerFactory().getMetamodel().entity( treatAsType )
			);
			this.original = original;
			this.treatAsType = treatAsType;
		}

		@Override
		public String getAlias() {
			return original.getAlias();
		}

		@Override
		public void prepareAlias(RenderingContext renderingContext) {
			// NOTE : we call `original#prepareAlias` here and during render
			//		since in some cases only one or the other will be called
			original.prepareAlias( renderingContext );
		}

		@Override
		public String render(RenderingContext renderingContext) {
			original.prepareAlias( renderingContext );
			return getTreatFragment();
		}

		protected String getTreatFragment() {
			return "treat(" + original.getAlias() + " as " + treatAsType.getName() + ")";
		}

		@Override
		public String getPathIdentifier() {
			return getTreatFragment();
		}

		@Override
		protected PathSource getPathSourceForSubPaths() {
			return this;
		}
	}

}
