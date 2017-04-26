/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql.navigable;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute.SingularAttributeClassification;
import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.produce.spi.ResolutionContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableSourceReference;
import org.hibernate.query.sqm.tree.from.SqmDowncast;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class PathResolverBasicImpl extends AbstractNavigableBindingResolver {
	private static final Logger log = Logger.getLogger( PathResolverBasicImpl.class );

	public PathResolverBasicImpl(ResolutionContext context) {
		super( context );
	}

	protected boolean shouldRenderTerminalAttributeBindingAsJoin() {
		return false;
	}

	@Override
	public boolean canReuseImplicitJoins() {
		return true;
	}

	@Override
	public SqmNavigableReference resolvePath(String... pathParts) {
		return resolveTreatedPath( null, pathParts );
	}

	@Override
	public SqmNavigableReference resolvePath(SqmNavigableSourceReference sourceBinding, String... pathParts) {
		return resolveTreatedPath( sourceBinding, null, pathParts );
	}

	private String[] sansFirstElement(String[] pathParts) {
		assert pathParts.length > 1;

		final String[] result = new String[pathParts.length - 1];
		System.arraycopy( pathParts, 1, result, 0, result.length );
		return result;
	}

	@Override
	public SqmNavigableReference resolveTreatedPath(EntityValuedExpressableType subclassIndicator, String... pathParts) {
		assert pathParts.length > 0;

		// The given pathParts indicate either:
		//		* a dot-identifier sequence whose root could either be
		//			* an identification variable
		//			* an attribute name exposed from a FromElement
		//		* a single identifier which could represent:
		//			*  an identification variable
		//			* an attribute name exposed from a FromElement

		if ( pathParts.length > 1 ) {
			// we had a dot-identifier sequence...

			// see if the root is an identification variable
			final SqmNavigableSourceReference identifiedBinding = (SqmNavigableSourceReference) context().getFromElementLocator()
					.findNavigableBindingByIdentificationVariable( pathParts[0] );
			if ( identifiedBinding != null ) {
				validatePathRoot( identifiedBinding );
				return resolveTreatedPath( identifiedBinding, subclassIndicator, sansFirstElement( pathParts ) );
			}

			// otherwise see if the root might be the name of an attribute exposed from a FromElement
			final SqmNavigableSourceReference root = (SqmNavigableSourceReference) context().getFromElementLocator()
					.findNavigableBindingExposingAttribute( pathParts[0] );
			if ( root != null ) {
				validatePathRoot( root );
				return resolveTreatedPath( root, subclassIndicator, pathParts );
			}
		}
		else {
			// we had a single identifier...

			// see if the identifier is an identification variable
			final SqmNavigableReference identifiedFromElement = context().getFromElementLocator()
					.findNavigableBindingByIdentificationVariable( pathParts[0] );
			if ( identifiedFromElement != null ) {
				return resolveFromElementAliasAsTerminal( (SqmFromExporter) identifiedFromElement );
			}

			// otherwise see if the identifier might be the name of an attribute exposed from a FromElement
			final SqmNavigableReference root = context().getFromElementLocator().findNavigableBindingExposingAttribute( pathParts[0] );
			if ( root != null ) {
				// todo : consider passing along subclassIndicator
				return resolveTerminalAttributeBinding( (SqmNavigableSourceReference) root, pathParts[0], subclassIndicator );
			}
		}

		return null;
	}

	protected void validatePathRoot(SqmNavigableReference root) {
	}

	@Override
	public SqmNavigableReference resolveTreatedPath(
			SqmNavigableSourceReference sourceBinding,
			EntityValuedExpressableType subclassIndicator,
			String... pathParts) {
		final SqmNavigableSourceReference intermediateJoinBindings = resolveAnyIntermediateAttributePathJoins( sourceBinding, pathParts );
		return resolveTerminalAttributeBinding( intermediateJoinBindings, pathParts[pathParts.length-1] );
	}

	protected SqmNavigableReference resolveTerminalAttributeBinding(
			SqmNavigableSourceReference sourceBinding,
			String terminalName) {
		return resolveTerminalAttributeBinding(
				sourceBinding,
				terminalName,
				null
		);
	}

	protected SqmNavigableReference resolveTerminalAttributeBinding(
			SqmNavigableSourceReference sourceBinding,
			String terminalName,
			EntityValuedExpressableType intrinsicSubclassIndicator) {
		final Navigable navigable = resolveNavigable( sourceBinding, terminalName );
		if ( shouldRenderTerminalAttributeBindingAsJoin() && isJoinable( navigable ) ) {
			log.debugf(
					"Resolved terminal navigable-binding [%s.%s ->%s] as navigable-join",
					sourceBinding.asLoggableText(),
					terminalName,
					navigable
			);
			return buildAttributeJoin(
					// see note in #resolveTreatedTerminal regarding cast
					sourceBinding,
					navigable,
					intrinsicSubclassIndicator
			);
		}
		else {
			log.debugf(
					"Resolved terminal navigable-binding [%s.%s ->%s] as navigable-reference",
					sourceBinding.asLoggableText(),
					terminalName,
					navigable
			);

			// todo (6.0) : we should probably force these to forcefully resolve the join.
			return context().getParsingContext().findOrCreateNavigableBinding(
					sourceBinding,
					navigable
			);
		}
	}

	private boolean isJoinable(Navigable attribute) {
		if ( SingularPersistentAttribute.class.isInstance( attribute ) ) {
			final SingularPersistentAttribute attrRef = (SingularPersistentAttribute) attribute;
			return attrRef.getAttributeTypeClassification() == SingularAttributeClassification.EMBEDDED
					|| attrRef.getAttributeTypeClassification() == SingularAttributeClassification.MANY_TO_ONE
					|| attrRef.getAttributeTypeClassification() == SingularAttributeClassification.ONE_TO_ONE;
		}
		else {
			// plural attributes can always be joined.
			return true;
		}
	}

	protected SqmNavigableReference resolveFromElementAliasAsTerminal(SqmFromExporter exporter) {
		log.debugf(
				"Resolved terminal as from-element alias : %s",
				exporter.getExportedFromElement().getIdentificationVariable()
		);
		return exporter.getExportedFromElement().getBinding();
	}

	protected SqmNavigableReference resolveTreatedTerminal(
			ResolutionContext context,
			SqmNavigableSourceReference lhs,
			String terminalName,
			EntityValuedExpressableType subclassIndicator) {
		final Navigable joinedAttribute = resolveNavigable( lhs, terminalName );
		log.debugf( "Resolved terminal treated-path : %s -> %s", joinedAttribute, subclassIndicator );
		final SqmAttributeReference joinBinding = (SqmAttributeReference) buildAttributeJoin(
				// todo : just do a cast for now, but this needs to be thought out (Binding -> SqmFrom)
				//		^^ SqmFrom specifically needed mainly needed for "FromElementSpace"
				//		but perhaps that resolution could be delayed
				lhs,
				joinedAttribute,
				subclassIndicator
		);

		joinBinding.addDowncast( new SqmDowncast( subclassIndicator ) );

		return new TreatedNavigableReference( joinBinding, subclassIndicator );
	}
}
