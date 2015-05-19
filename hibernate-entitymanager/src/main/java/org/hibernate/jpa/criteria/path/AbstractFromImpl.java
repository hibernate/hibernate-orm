/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.criteria.path;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.criteria.CollectionJoin;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.SetJoin;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.CollectionAttribute;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.MapAttribute;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SetAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.jpa.criteria.BasicPathUsageException;
import org.hibernate.jpa.criteria.CollectionJoinImplementor;
import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.CriteriaSubqueryImpl;
import org.hibernate.jpa.criteria.FromImplementor;
import org.hibernate.jpa.criteria.JoinImplementor;
import org.hibernate.jpa.criteria.ListJoinImplementor;
import org.hibernate.jpa.criteria.MapJoinImplementor;
import org.hibernate.jpa.criteria.PathSource;
import org.hibernate.jpa.criteria.SetJoinImplementor;
import org.hibernate.jpa.criteria.compile.RenderingContext;

/**
 * Convenience base class for various {@link javax.persistence.criteria.From} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractFromImpl<Z, X>
		extends AbstractPathImpl<X>
		implements From<Z, X>, FromImplementor<Z, X>, Serializable {

	public static final JoinType DEFAULT_JOIN_TYPE = JoinType.INNER;

	private Set<Join<X, ?>> joins;
	private Set<Fetch<X, ?>> fetches;

	public AbstractFromImpl(CriteriaBuilderImpl criteriaBuilder, Class<X> javaType) {
		this( criteriaBuilder, javaType, null );
	}

	public AbstractFromImpl(CriteriaBuilderImpl criteriaBuilder, Class<X> javaType, PathSource pathSource) {
		super( criteriaBuilder, javaType, pathSource );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public PathSource<Z> getPathSource() {
		return super.getPathSource();
	}

	@Override
	public String getPathIdentifier() {
		return getAlias();
	}

	@Override
	protected boolean canBeDereferenced() {
		return true;
	}

	@Override
	public void prepareAlias(RenderingContext renderingContext) {
		if ( getAlias() == null ) {
			if ( isCorrelated() ) {
				setAlias( getCorrelationParent().getAlias() );
			}
			else {
				setAlias( renderingContext.generateAlias() );
			}
		}
	}

	@Override
	public String renderProjection(RenderingContext renderingContext) {
		prepareAlias( renderingContext );
		return getAlias();
	}

	@Override
	public String render(RenderingContext renderingContext) {
		return renderProjection( renderingContext );
	}

	@Override
	public Attribute<?, ?> getAttribute() {
		return null;
	}

	public From<?, Z> getParent() {
		return null;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	protected Attribute<X, ?> locateAttributeInternal(String name) {
		return (Attribute<X, ?>) locateManagedType().getAttribute( name );
	}

	@SuppressWarnings({"unchecked"})
	protected ManagedType<? super X> locateManagedType() {
		// by default, this should be the model
		return (ManagedType<? super X>) getModel();
	}


	// CORRELATION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	// IMPL NOTE : another means from handling correlations is to create a series of
	//		specialized From implementations that represent the correlation roots.  While
	//		that may be cleaner code-wise, it is certainly means creating a lot of "extra"
	//		classes since we'd need one for each Subquery#correlate method

	private FromImplementor<Z, X> correlationParent;

	private JoinScope<X> joinScope = new BasicJoinScope();

	/**
	 * Helper contract used to define who/what keeps track of joins and fetches made from this <tt>FROM</tt>.
	 */
	public static interface JoinScope<X> extends Serializable {
		public void addJoin(Join<X, ?> join);

		public void addFetch(Fetch<X, ?> fetch);
	}

	protected class BasicJoinScope implements JoinScope<X> {
		@Override
		public void addJoin(Join<X, ?> join) {
			if ( joins == null ) {
				joins = new LinkedHashSet<Join<X, ?>>();
			}
			joins.add( join );
		}

		@Override
		public void addFetch(Fetch<X, ?> fetch) {
			if ( fetches == null ) {
				fetches = new LinkedHashSet<Fetch<X, ?>>();
			}
			fetches.add( fetch );
		}
	}

	protected class CorrelationJoinScope implements JoinScope<X> {
		@Override
		public void addJoin(Join<X, ?> join) {
			if ( joins == null ) {
				joins = new LinkedHashSet<Join<X, ?>>();
			}
			joins.add( join );
		}

		@Override
		public void addFetch(Fetch<X, ?> fetch) {
			throw new UnsupportedOperationException( "Cannot define fetch from a subquery correlation" );
		}
	}

	@Override
	public boolean isCorrelated() {
		return correlationParent != null;
	}

	@Override
	public FromImplementor<Z, X> getCorrelationParent() {
		if ( correlationParent == null ) {
			throw new IllegalStateException(
					String.format(
							"Criteria query From node [%s] is not part of a subquery correlation",
							getPathIdentifier()
					)
			);
		}
		return correlationParent;
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public FromImplementor<Z, X> correlateTo(CriteriaSubqueryImpl subquery) {
		final FromImplementor<Z, X> correlationDelegate = createCorrelationDelegate();
		correlationDelegate.prepareCorrelationDelegate( this );
		return correlationDelegate;
	}

	protected abstract FromImplementor<Z, X> createCorrelationDelegate();

	@Override
	public void prepareCorrelationDelegate(FromImplementor<Z, X> parent) {
		this.joinScope = new CorrelationJoinScope();
		this.correlationParent = parent;
	}

	@Override
	public String getAlias() {
		return isCorrelated() ? getCorrelationParent().getAlias() : super.getAlias();
	}

	// JOINS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected abstract boolean canBeJoinSource();

	protected RuntimeException illegalJoin() {
		return new IllegalArgumentException(
				"Collection of values [" + getPathIdentifier() + "] cannot be source of a join"
		);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Set<Join<X, ?>> getJoins() {
		return joins == null
				? Collections.EMPTY_SET
				: joins;
	}

	@Override
	public <Y> Join<X, Y> join(SingularAttribute<? super X, Y> singularAttribute) {
		return join( singularAttribute, DEFAULT_JOIN_TYPE );
	}

	@Override
	public <Y> Join<X, Y> join(SingularAttribute<? super X, Y> attribute, JoinType jt) {
		if ( !canBeJoinSource() ) {
			throw illegalJoin();
		}

		Join<X, Y> join = constructJoin( attribute, jt );
		joinScope.addJoin( join );
		return join;
	}

	private <Y> JoinImplementor<X, Y> constructJoin(SingularAttribute<? super X, Y> attribute, JoinType jt) {
		if ( Type.PersistenceType.BASIC.equals( attribute.getType().getPersistenceType() ) ) {
			throw new BasicPathUsageException( "Cannot join to attribute of basic type", attribute );
		}

		// TODO : runtime check that the attribute in fact belongs to this From's model/bindable

		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final Class<Y> attributeType = attribute.getBindableJavaType();
		return new SingularAttributeJoin<X, Y>(
				criteriaBuilder(),
				attributeType,
				this,
				attribute,
				jt
		);
	}

	@Override
	public <Y> CollectionJoin<X, Y> join(CollectionAttribute<? super X, Y> collection) {
		return join( collection, DEFAULT_JOIN_TYPE );
	}

	@Override
	public <Y> CollectionJoin<X, Y> join(CollectionAttribute<? super X, Y> collection, JoinType jt) {
		if ( !canBeJoinSource() ) {
			throw illegalJoin();
		}

		final CollectionJoin<X, Y> join = constructJoin( collection, jt );
		joinScope.addJoin( join );
		return join;
	}

	private <Y> CollectionJoinImplementor<X, Y> constructJoin(
			CollectionAttribute<? super X, Y> collection,
			JoinType jt) {
		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		// TODO : runtime check that the attribute in fact belongs to this From's model/bindable

		final Class<Y> attributeType = collection.getBindableJavaType();
		return new CollectionAttributeJoin<X, Y>(
				criteriaBuilder(),
				attributeType,
				this,
				collection,
				jt
		);
	}

	@Override
	public <Y> SetJoin<X, Y> join(SetAttribute<? super X, Y> set) {
		return join( set, DEFAULT_JOIN_TYPE );
	}

	@Override
	public <Y> SetJoin<X, Y> join(SetAttribute<? super X, Y> set, JoinType jt) {
		if ( !canBeJoinSource() ) {
			throw illegalJoin();
		}

		final SetJoin<X, Y> join = constructJoin( set, jt );
		joinScope.addJoin( join );
		return join;
	}

	private <Y> SetJoinImplementor<X, Y> constructJoin(SetAttribute<? super X, Y> set, JoinType jt) {
		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		// TODO : runtime check that the attribute in fact belongs to this From's model/bindable

		final Class<Y> attributeType = set.getBindableJavaType();
		return new SetAttributeJoin<X, Y>( criteriaBuilder(), attributeType, this, set, jt );
	}

	@Override
	public <Y> ListJoin<X, Y> join(ListAttribute<? super X, Y> list) {
		return join( list, DEFAULT_JOIN_TYPE );
	}

	@Override
	public <Y> ListJoin<X, Y> join(ListAttribute<? super X, Y> list, JoinType jt) {
		if ( !canBeJoinSource() ) {
			throw illegalJoin();
		}

		final ListJoin<X, Y> join = constructJoin( list, jt );
		joinScope.addJoin( join );
		return join;
	}

	private <Y> ListJoinImplementor<X, Y> constructJoin(ListAttribute<? super X, Y> list, JoinType jt) {
		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		// TODO : runtime check that the attribute in fact belongs to this From's model/bindable

		final Class<Y> attributeType = list.getBindableJavaType();
		return new ListAttributeJoin<X, Y>( criteriaBuilder(), attributeType, this, list, jt );
	}

	@Override
	public <K, V> MapJoin<X, K, V> join(MapAttribute<? super X, K, V> map) {
		return join( map, DEFAULT_JOIN_TYPE );
	}

	@Override
	public <K, V> MapJoin<X, K, V> join(MapAttribute<? super X, K, V> map, JoinType jt) {
		if ( !canBeJoinSource() ) {
			throw illegalJoin();
		}

		final MapJoin<X, K, V> join = constructJoin( map, jt );
		joinScope.addJoin( join );
		return join;
	}

	private <K, V> MapJoinImplementor<X, K, V> constructJoin(MapAttribute<? super X, K, V> map, JoinType jt) {
		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		// TODO : runtime check that the attribute in fact belongs to this From's model/bindable

		final Class<V> attributeType = map.getBindableJavaType();
		return new MapAttributeJoin<X, K, V>( criteriaBuilder(), attributeType, this, map, jt );
	}

	@Override
	public <X, Y> Join<X, Y> join(String attributeName) {
		return join( attributeName, DEFAULT_JOIN_TYPE );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X, Y> Join<X, Y> join(String attributeName, JoinType jt) {
		if ( !canBeJoinSource() ) {
			throw illegalJoin();
		}

		if ( jt.equals( JoinType.RIGHT ) ) {
			throw new UnsupportedOperationException( "RIGHT JOIN not supported" );
		}

		final Attribute<X, ?> attribute = (Attribute<X, ?>) locateAttribute( attributeName );
		if ( attribute.isCollection() ) {
			final PluralAttribute pluralAttribute = (PluralAttribute) attribute;
			if ( PluralAttribute.CollectionType.COLLECTION.equals( pluralAttribute.getCollectionType() ) ) {
				return (Join<X, Y>) join( (CollectionAttribute) attribute, jt );
			}
			else if ( PluralAttribute.CollectionType.LIST.equals( pluralAttribute.getCollectionType() ) ) {
				return (Join<X, Y>) join( (ListAttribute) attribute, jt );
			}
			else if ( PluralAttribute.CollectionType.SET.equals( pluralAttribute.getCollectionType() ) ) {
				return (Join<X, Y>) join( (SetAttribute) attribute, jt );
			}
			else {
				return (Join<X, Y>) join( (MapAttribute) attribute, jt );
			}
		}
		else {
			return (Join<X, Y>) join( (SingularAttribute) attribute, jt );
		}
	}

	@Override
	public <X, Y> CollectionJoin<X, Y> joinCollection(String attributeName) {
		return joinCollection( attributeName, DEFAULT_JOIN_TYPE );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X, Y> CollectionJoin<X, Y> joinCollection(String attributeName, JoinType jt) {
		final Attribute<X, ?> attribute = (Attribute<X, ?>) locateAttribute( attributeName );
		if ( !attribute.isCollection() ) {
			throw new IllegalArgumentException( "Requested attribute was not a collection" );
		}

		final PluralAttribute pluralAttribute = (PluralAttribute) attribute;
		if ( !PluralAttribute.CollectionType.COLLECTION.equals( pluralAttribute.getCollectionType() ) ) {
			throw new IllegalArgumentException( "Requested attribute was not a collection" );
		}

		return (CollectionJoin<X, Y>) join( (CollectionAttribute) attribute, jt );
	}

	@Override
	public <X, Y> SetJoin<X, Y> joinSet(String attributeName) {
		return joinSet( attributeName, DEFAULT_JOIN_TYPE );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X, Y> SetJoin<X, Y> joinSet(String attributeName, JoinType jt) {
		final Attribute<X, ?> attribute = (Attribute<X, ?>) locateAttribute( attributeName );
		if ( !attribute.isCollection() ) {
			throw new IllegalArgumentException( "Requested attribute was not a set" );
		}

		final PluralAttribute pluralAttribute = (PluralAttribute) attribute;
		if ( !PluralAttribute.CollectionType.SET.equals( pluralAttribute.getCollectionType() ) ) {
			throw new IllegalArgumentException( "Requested attribute was not a set" );
		}

		return (SetJoin<X, Y>) join( (SetAttribute) attribute, jt );
	}

	@Override
	public <X, Y> ListJoin<X, Y> joinList(String attributeName) {
		return joinList( attributeName, DEFAULT_JOIN_TYPE );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X, Y> ListJoin<X, Y> joinList(String attributeName, JoinType jt) {
		final Attribute<X, ?> attribute = (Attribute<X, ?>) locateAttribute( attributeName );
		if ( !attribute.isCollection() ) {
			throw new IllegalArgumentException( "Requested attribute was not a list" );
		}

		final PluralAttribute pluralAttribute = (PluralAttribute) attribute;
		if ( !PluralAttribute.CollectionType.LIST.equals( pluralAttribute.getCollectionType() ) ) {
			throw new IllegalArgumentException( "Requested attribute was not a list" );
		}

		return (ListJoin<X, Y>) join( (ListAttribute) attribute, jt );
	}

	@Override
	public <X, K, V> MapJoin<X, K, V> joinMap(String attributeName) {
		return joinMap( attributeName, DEFAULT_JOIN_TYPE );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X, K, V> MapJoin<X, K, V> joinMap(String attributeName, JoinType jt) {
		final Attribute<X, ?> attribute = (Attribute<X, ?>) locateAttribute( attributeName );
		if ( !attribute.isCollection() ) {
			throw new IllegalArgumentException( "Requested attribute was not a map" );
		}

		final PluralAttribute pluralAttribute = (PluralAttribute) attribute;
		if ( !PluralAttribute.CollectionType.MAP.equals( pluralAttribute.getCollectionType() ) ) {
			throw new IllegalArgumentException( "Requested attribute was not a map" );
		}

		return (MapJoin<X, K, V>) join( (MapAttribute) attribute, jt );
	}


	// FETCHES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	protected boolean canBeFetchSource() {
		// the conditions should be the same...
		return canBeJoinSource();
	}

	protected RuntimeException illegalFetch() {
		return new IllegalArgumentException(
				"Collection of values [" + getPathIdentifier() + "] cannot be source of a fetch"
		);
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public Set<Fetch<X, ?>> getFetches() {
		return fetches == null
				? Collections.EMPTY_SET
				: fetches;
	}

	@Override
	public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> singularAttribute) {
		return fetch( singularAttribute, DEFAULT_JOIN_TYPE );
	}

	@Override
	public <Y> Fetch<X, Y> fetch(SingularAttribute<? super X, Y> attribute, JoinType jt) {
		if ( !canBeFetchSource() ) {
			throw illegalFetch();
		}

		Fetch<X, Y> fetch = constructJoin( attribute, jt );
		joinScope.addFetch( fetch );
		return fetch;
	}

	@Override
	public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> pluralAttribute) {
		return fetch( pluralAttribute, DEFAULT_JOIN_TYPE );
	}

	@Override
	public <Y> Fetch<X, Y> fetch(PluralAttribute<? super X, ?, Y> pluralAttribute, JoinType jt) {
		if ( !canBeFetchSource() ) {
			throw illegalFetch();
		}

		final Fetch<X, Y> fetch;
		// TODO : combine Fetch and Join hierarchies (JoinImplementor extends Join,Fetch???)
		if ( PluralAttribute.CollectionType.COLLECTION.equals( pluralAttribute.getCollectionType() ) ) {
			fetch = constructJoin( (CollectionAttribute<X, Y>) pluralAttribute, jt );
		}
		else if ( PluralAttribute.CollectionType.LIST.equals( pluralAttribute.getCollectionType() ) ) {
			fetch = constructJoin( (ListAttribute<X, Y>) pluralAttribute, jt );
		}
		else if ( PluralAttribute.CollectionType.SET.equals( pluralAttribute.getCollectionType() ) ) {
			fetch = constructJoin( (SetAttribute<X, Y>) pluralAttribute, jt );
		}
		else {
			fetch = constructJoin( (MapAttribute<X, ?, Y>) pluralAttribute, jt );
		}
		joinScope.addFetch( fetch );
		return fetch;
	}

	@Override
	public <X, Y> Fetch<X, Y> fetch(String attributeName) {
		return fetch( attributeName, DEFAULT_JOIN_TYPE );
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <X, Y> Fetch<X, Y> fetch(String attributeName, JoinType jt) {
		if ( !canBeFetchSource() ) {
			throw illegalFetch();
		}

		Attribute<X, ?> attribute = (Attribute<X, ?>) locateAttribute( attributeName );
		if ( attribute.isCollection() ) {
			return (Fetch<X, Y>) fetch( (PluralAttribute) attribute, jt );
		}
		else {
			return (Fetch<X, Y>) fetch( (SingularAttribute) attribute, jt );
		}
	}
}
