/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal.hql.navigable;

import java.util.Locale;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute.SingularAttributeClassification;
import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.persister.queryable.NavigableResolutionException;
import org.hibernate.query.sqm.produce.spi.ResolutionContext;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableSourceReference;

/**
 * Template support for PathResolver implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractNavigableBindingResolver implements NavigableBindingResolver {
	private final ResolutionContext context;

	public AbstractNavigableBindingResolver(ResolutionContext context) {
		this.context = context;
	}

	protected ResolutionContext context() {
		return context;
	}

	protected SqmNavigableSourceReference resolveAnyIntermediateAttributePathJoins(
			SqmNavigableSourceReference sourceBinding,
			String[] pathParts) {
		// build joins for any intermediate path parts
		for ( int i = 0, max = pathParts.length-1; i < max; i++ ) {
			sourceBinding = buildIntermediateAttributeJoin( sourceBinding, pathParts[i] );
		}
		return sourceBinding;
	}

	protected SqmNavigableSourceReference buildIntermediateAttributeJoin(
			SqmNavigableSourceReference sourceBinding,
			String navigableName) {
		final Navigable intermediateNavigable = resolveNavigable( sourceBinding, navigableName );

		validateIntermediateAttributeJoin( sourceBinding, intermediateNavigable );

		return (SqmNavigableSourceReference) buildAttributeJoin( sourceBinding, intermediateNavigable, null );
	}

	protected SqmNavigableReference buildAttributeJoin(
			SqmNavigableSourceReference sourceBinding,
			Navigable joinedNavigable,
			EntityValuedExpressableType subclassIndicator) {
		final SqmAttributeReference attributeBinding = (SqmAttributeReference) context().getParsingContext()
				.findOrCreateNavigableBinding( sourceBinding, joinedNavigable );

		if ( attributeBinding.getExportedFromElement() == null ) {
			attributeBinding.injectExportedFromElement(
					context().getFromElementBuilder().buildAttributeJoin(
							attributeBinding,
							null,
							subclassIndicator,
							getIntermediateJoinType(),
							areIntermediateJoinsFetched(),
							canReuseImplicitJoins()
					)
			);
		}

		return attributeBinding;
	}

	protected void validateIntermediateAttributeJoin(
			SqmNavigableSourceReference sourceBinding,
			Navigable joinedAttributeDescriptor) {
		if ( !SingularPersistentAttribute.class.isInstance( joinedAttributeDescriptor ) ) {
			throw new SemanticException(
					String.format(
							Locale.ROOT,
							"Attribute [%s -> %s] is plural, cannot be used as non-terminal in path expression",
							sourceBinding.asLoggableText(),
							joinedAttributeDescriptor.getNavigableRole().getNavigableName()
					)
			);
		}
		else {
			// make sure it is Bindable
			final SingularPersistentAttribute singularAttribute = (SingularPersistentAttribute) joinedAttributeDescriptor;
			if ( !canBeDereferenced( singularAttribute.getAttributeTypeClassification() ) ) {
				throw new SemanticException(
						String.format(
								Locale.ROOT,
								"SingularAttribute [%s -> %s] reports is cannot be de-referenced, therefore cannot be used as non-terminal in path expression",
								sourceBinding.asLoggableText(),
								joinedAttributeDescriptor.getNavigableRole().getNavigableName()
						)
				);
			}
		}
	}

	private boolean canBeDereferenced(SingularAttributeClassification classification) {
		return classification == SingularAttributeClassification.EMBEDDED
				|| classification == SingularAttributeClassification.MANY_TO_ONE
				|| classification == SingularAttributeClassification.ONE_TO_ONE;
	}

	protected SqmJoinType getIntermediateJoinType() {
		return SqmJoinType.LEFT;
	}

	protected boolean areIntermediateJoinsFetched() {
		return false;
	}

	protected Navigable resolveNavigable(SqmNavigableSourceReference sourceBinding, String navigableName) {
		final Navigable navigable = sourceBinding.getReferencedNavigable().findNavigable( navigableName );
		if ( navigable == null ) {
			throw new NavigableResolutionException(
					"Could not locate navigable named [" + navigableName + "] relative to [" +
							sourceBinding.getReferencedNavigable().asLoggableText() + "]"
			);
		}
		return navigable;
	}

	protected void resolveAttributeJoinIfNot(SqmNavigableReference navigableBinding) {
		if ( !SqmAttributeReference.class.isInstance( navigableBinding ) ) {
			return;
		}

		SqmAttributeReference attributeBinding = (SqmAttributeReference) navigableBinding;
		if ( attributeBinding.getExportedFromElement() != null ) {
			return;
		}

		if ( !joinable( attributeBinding ) ) {
			return;
		}

		attributeBinding.injectExportedFromElement(
				context().getFromElementBuilder().buildAttributeJoin(
						attributeBinding,
						null,
						null,
						SqmJoinType.INNER,
						false,
						true
				)
		);
	}

	private boolean joinable(SqmAttributeReference attributeBinding) {
		if ( attributeBinding.getReferencedNavigable() instanceof SingularPersistentAttribute ) {
			final SingularPersistentAttribute attrRef = (SingularPersistentAttribute) attributeBinding.getReferencedNavigable();
			return attrRef.getAttributeTypeClassification() != SingularAttributeClassification.BASIC
					&& attrRef.getAttributeTypeClassification() != SingularAttributeClassification.ANY;
		}

		// Plural attributes are always joinable
		return true;
	}
}
