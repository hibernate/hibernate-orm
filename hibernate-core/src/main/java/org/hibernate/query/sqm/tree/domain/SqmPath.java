/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.JpaPath;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.SqmCreationHelper;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Models a reference to a part of the application's domain model (a Navigable)
 * as part of an SQM tree.
 *
 * This correlates roughly to the JPA Criteria notion of Path, hence the name.
 *
 * @author Steve Ebersole
 */
public interface SqmPath<T> extends SqmExpression<T>, SemanticPathPart, JpaPath<T> {

	/**
	 * Returns the NavigablePath.
	 */
	NavigablePath getNavigablePath();

	/**
	 * The path source that this path refers to (and that most likely
	 * created it).
	 *
	 * @see SqmPathSource#createSqmPath
	 */
	SqmPathSource<?> getReferencedPathSource();

	/**
	 * Retrieve the explicit alias, if one.  May return null
	 */
	String getExplicitAlias();

	void setExplicitAlias(String explicitAlias);

	/**
	 * Get the left-hand side of this path - may be null, indicating a
	 * root, cross-join or entity-join
	 */
	SqmPath<?> getLhs();

	@Override
	SqmPathSource<T> getNodeType();

	@Override
	default void applyInferableType(SqmExpressable<?> type) {
		// do nothing
	}

	@Override
	default JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return getNodeType().getExpressableJavaTypeDescriptor();
	}

	@Override
	<S extends T> SqmTreatedPath<T,S> treatAs(Class<S> treatJavaType) throws PathException;

	@Override
	<S extends T> SqmTreatedPath<T,S> treatAs(EntityDomainType<S> treatTarget) throws PathException;

	default SqmRoot findRoot() {
		final SqmPath lhs = getLhs();
		if ( lhs != null ) {
			return lhs.findRoot();
		}

		throw new ParsingException( "Could not find root" );
	}

	@Override
	default SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException( "Non-plural path [" + currentContextKey + "] cannot be index-accessed" );
	}

	/**
	 * Perform any preparations needed to process the named Navigable.  Create joins?
	 *
	 * This should equate to resolution of implicit joins.  Given
	 * `select p.address.city from Person p ....`, e.g.,  we'd end up with the following:
	 *
	 * 		1) 	Because we process the from-clause first, `Person p` is already available as an
	 * 		   	SqmRoot with NavigablePath[Person(p)]
	 * 		2) 	As we process the select-clause, the `p.address.city` dot-ident sequence is processed
	 * 		   	by the registered `DotIdentifierConsumer`
	 *
	 *			1)	the first part (`p`) is resolved, internally, as the registered SqmRoot as an alias
	 *				which is tracked there as its "current `SemanticPathPart`"
	 *			2)	each "continuation"	(here `address` and then `city`) is handled by applying that
	 *				name to the "current `SemanticPathPart`", assigning its result back as the new
	 *				"current `SemanticPathPart`"
	 *
	 *					1) `address` is resolved against SqmRoot, producing a `SqmEmbeddedValuedSimplePath`
	 *						with NavigablePath[Person(p).address].  That is registered in the PathRegistry
	 *						in `#sqmPathMap`, but not (yet) in `#sqmFromPath`.
	 *					2)	`city` is resolved against the SqmEmbeddedValuedSimplePath(NavigablePath[Person(p).address]).
	 *						This triggers a few things:
	 *
	 *						1) 	SqmEmbeddedValuedSimplePath( Person(p).address ) is given a
	 *							chance to prepare itself to be used as the LHS via this `#prepareForSubNavigableReference`
	 *							method.  Here, we use that opportunity to create the implicit SqmNavigableJoin for
	 *							the same `Person(p).address` path.  We register this join form with the PathRegistry
	 *							which "over-writes" the previous `SqmEmbeddedValuedSimplePath` registration
	 *						2)	Processing `city` produces a `SqmBasicValuedSimplePath( Person(p).address.city )`
	 *							which is registered in the PathRegistry, again just in `#sqmPathMap`, but not (yet)
	 *							in `#sqmFromPath`
	 *
	 * 		At this point processing would return from `DotIdentifierConsumer` back to `SemanticQueryBuilder`
	 * 		where we call `DotIdentifierConsumer#getConsumedPart` to get the last "current part"
	 * 		`SqmBasicValuedSimplePath( Person(p).address.city )` as the result for the fully resolved
	 * 		dot-ident-sequence
	 *
	 * 	todo (6.0) : ideally we'd delay this until SQM -> SQL AST conversion : criteria-as-SQM
	 */
	default void prepareForSubNavigableReference(
			SqmPathSource<?> subNavigable,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		SqmCreationHelper.resolveAsLhs( getLhs(), this, subNavigable, isSubReferenceTerminal, creationState );
	}

//	/**
//	 * Treat this path as the given type.  "Cast it" to the target type.
//	 *
//	 * May throw an exception if the Path is not treatable as the requested type.
//	 *
//	 * Also recognizes any {@link Navigable} target type and applies it to the
//	 * {@link #getReferencedPathSource()}.
//	 *
//	 * @apiNote This is very different from JPA's {@link #as} (and variants like
//	 * {@link #asInteger()}, etc) which are equivalent to SQL CAST function calls.
//	 *
//	 * @return The "casted" reference
//	 */
//	@SuppressWarnings("unchecked")
//	default <X> X sqmAs(Class<X> targetType) {
//		if ( targetType.isInstance( this ) ) {
//			return (X) this;
//		}
//
//		if ( getReferencedPathSource().getSqmPathType()
//				.getExpressableJavaTypeDescriptor()
//				.getJavaType()
//				.isAssignableFrom( targetType ) ) {
//			return (X) ( (Navigable) getReferencedPathSource() ).as( targetType );
//		}
//
//		throw new IllegalArgumentException(
//				String.format(
//						Locale.ROOT,
//						"`%s` cannot be treated as `%s`",
//						getClass().getName(),
//						targetType.getName()
//				)
//		);
//	}
//
//	default <X> X sqmAs(Class<X> targetType, Supplier<RuntimeException> exceptionSupplier) {
//		try {
//			return sqmAs( targetType );
//		}
//		catch (IllegalArgumentException e) {
//			throw exceptionSupplier.get();
//		}
//	}
}
