/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.persister.collection.spi.CollectionElement;
import org.hibernate.persister.collection.spi.CollectionIndex;
import org.hibernate.persister.common.internal.SingularPersistentAttributeBasic;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEntity;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.PluralPersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.persister.entity.spi.IdentifierDescriptorComposite;
import org.hibernate.persister.entity.spi.IdentifierDescriptorSimple;
import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.domain.SqmPluralAttributeIndex;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReferenceComposite;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReferenceSimple;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityTypedReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmIndexedElementReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmIndexedElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmIndexedElementReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxElementReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxIndexReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmMaxIndexReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinElementReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceEmbeddable;
import org.hibernate.query.sqm.tree.expression.domain.SqmMinIndexReferenceEntity;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableSourceReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceBasic;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReferenceEntity;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;

/**
 * @author Steve Ebersole
 */
public class NavigableBindingHelper {
	public static SqmFrom resolveExportedFromElement(SqmNavigableReference binding) {
		if ( binding instanceof SqmFromExporter ) {
			return ( (SqmFromExporter) binding ).getExportedFromElement();
		}

		if ( binding.getSourceReference() != null ) {
			return resolveExportedFromElement( binding.getSourceReference() );
		}

		throw new ParsingException( "Could not resolve SqmFrom element from NavigableBinding : " + binding );
	}

	public static SqmFromElementSpace extractSpace(SqmFromExporter exporter) {
		return exporter.getExportedFromElement() == null ? null : exporter.getExportedFromElement() .getContainingSpace();
	}

	public static SqmNavigableReference createNavigableBinding(SqmNavigableSourceReference source, Navigable navigable) {
		if ( navigable instanceof IdentifierDescriptor ) {
			assert source instanceof SqmEntityTypedReference;
			return createEntityIdentiferBinding( (SqmEntityTypedReference) source, (IdentifierDescriptor) navigable );
		}
		else if ( navigable instanceof SingularPersistentAttribute ) {
			return createSingularAttributeBinding( source, (SingularPersistentAttribute) navigable );
		}
		else if ( navigable instanceof PluralPersistentAttribute ) {
			return createPluralAttributeBinding( source, (PluralPersistentAttribute) navigable );
		}
		else if ( navigable instanceof CollectionElement ) {
			return createCollectionElementBinding( source, (CollectionElement) navigable );
		}
		else if ( navigable instanceof CollectionIndex ) {
			return createCollectionIndexBinding( source, (CollectionIndex) navigable );
		}
		else if ( navigable instanceof EntityValuedExpressableType ) {
			// for anything else source should be null
			assert source == null;
			return createEntityBinding( (EntityValuedExpressableType) navigable );
		}

		throw new ParsingException( "Unexpected SqmNavigable for creation of NavigableBinding : " + navigable );
	}

	private static SqmEntityIdentifierReference createEntityIdentiferBinding(
			SqmEntityTypedReference sourceBinding,
			IdentifierDescriptor navigable) {
		if ( navigable instanceof IdentifierDescriptorSimple ) {
			return new SqmEntityIdentifierReferenceSimple( sourceBinding, (IdentifierDescriptorSimple) navigable );
		}
		else {
			return new SqmEntityIdentifierReferenceComposite( sourceBinding, (IdentifierDescriptorComposite) navigable );
		}
	}

	private static SqmPluralAttributeReference createPluralAttributeBinding(
			SqmNavigableSourceReference lhs,
			PluralPersistentAttribute pluralAttribute) {
		return new SqmPluralAttributeReference( lhs, pluralAttribute );
	}


	public static SqmSingularAttributeReference createSingularAttributeBinding(
			SqmNavigableSourceReference sourceBinding,
			SingularPersistentAttribute attribute) {
		switch ( attribute.getAttributeTypeClassification() ) {
			case BASIC: {
				return new SqmSingularAttributeReferenceBasic(
						sourceBinding,
						(SingularPersistentAttributeBasic) attribute
				);
			}
			case EMBEDDED: {
				return new SqmSingularAttributeReferenceEmbedded(
						sourceBinding,
						(SingularPersistentAttributeEmbedded) attribute
				);
			}
			case ONE_TO_ONE:
			case MANY_TO_ONE: {
				return new SqmSingularAttributeReferenceEntity(
						sourceBinding,
						(SingularPersistentAttributeEntity) attribute
				);
			}
			default: {
				throw new NotYetImplementedException();
			}
		}
	}

	public static SqmCollectionElementReference createCollectionElementBinding(
			SqmNavigableSourceReference source,
			CollectionElement elementDescriptor) {
		assert source instanceof SqmPluralAttributeReference;
		final SqmPluralAttributeReference pluralAttributeBinding = (SqmPluralAttributeReference) source;

		switch ( elementDescriptor.getClassification() ) {
			case BASIC: {
				return new SqmCollectionElementReferenceBasic( pluralAttributeBinding );
			}
			case EMBEDDABLE: {
				return new SqmCollectionElementReferenceEmbedded( pluralAttributeBinding );
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				return new SqmCollectionElementReferenceEntity( pluralAttributeBinding );
			}
			default: {
				throw new NotYetImplementedException();
			}
		}
	}

	enum CollectionPartBindingType {
		NORMAL,
		MIN,
		MAX
	}

	public static SqmCollectionElementReference createCollectionElementBinding(
			CollectionPartBindingType bindingType,
			SqmNavigableSourceReference source,
			CollectionElement elementDescriptor) {
		assert source instanceof SqmPluralAttributeReference;
		final SqmPluralAttributeReference pluralAttributeBinding = (SqmPluralAttributeReference) source;

		switch ( elementDescriptor.getClassification() ) {
			case BASIC: {
				switch ( bindingType ) {
					case MAX: {
						return new SqmMaxElementReferenceBasic( pluralAttributeBinding );
					}
					case MIN: {
						return new SqmMinElementReferenceBasic( pluralAttributeBinding );
					}
					default: {
						return new SqmCollectionElementReferenceBasic( pluralAttributeBinding );
					}
				}
			}
			case EMBEDDABLE: {
				switch ( bindingType ) {
					case MAX: {
						return new SqmMaxElementReferenceEmbedded( pluralAttributeBinding );
					}
					case MIN: {
						return new SqmMinElementReferenceEmbedded( pluralAttributeBinding );
					}
					default: {
						return new SqmCollectionElementReferenceEmbedded( pluralAttributeBinding );
					}
				}
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				switch ( bindingType ) {
					case MAX: {
						return new SqmMaxElementReferenceEntity( pluralAttributeBinding );
					}
					case MIN: {
						return new SqmMinElementReferenceEntity( pluralAttributeBinding );
					}
					default: {
						return new SqmCollectionElementReferenceEntity( pluralAttributeBinding );
					}
				}
			}
			default: {
				throw new NotYetImplementedException();
			}
		}
	}

	public static SqmCollectionIndexReference createCollectionIndexBinding(
			SqmNavigableSourceReference source,
			CollectionIndex indexDescriptor) {
		assert source instanceof SqmPluralAttributeReference;
		final SqmPluralAttributeReference pluralAttributeBinding = (SqmPluralAttributeReference) source;

		switch ( indexDescriptor.getClassification() ) {
			case BASIC: {
				return new SqmCollectionIndexReferenceBasic( pluralAttributeBinding );
			}
			case EMBEDDABLE: {
				return new SqmCollectionIndexReferenceEmbedded( pluralAttributeBinding );
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				return new SqmCollectionIndexReferenceEntity( pluralAttributeBinding );
			}
			default: {
				throw new NotYetImplementedException(  );
			}
		}
	}


	public static SqmCollectionIndexReference createCollectionIndexBinding(
			CollectionPartBindingType bindingType,
			SqmNavigableSourceReference source,
			SqmPluralAttributeIndex indexDescriptor) {
		assert source instanceof SqmPluralAttributeReference;
		final SqmPluralAttributeReference pluralAttributeBinding = (SqmPluralAttributeReference) source;

		switch ( indexDescriptor.getClassification() ) {
			case BASIC: {
				switch ( bindingType ) {
					case MAX: {
						return new SqmMaxIndexReferenceBasic( pluralAttributeBinding );
					}
					case MIN: {
						return new SqmMinIndexReferenceBasic( pluralAttributeBinding );
					}
					default: {
						return new SqmCollectionIndexReferenceBasic( pluralAttributeBinding );
					}
				}
			}
			case EMBEDDABLE: {
				switch ( bindingType ) {
					case MAX: {
						return new SqmMaxIndexReferenceEmbedded( pluralAttributeBinding );
					}
					case MIN: {
						return new SqmMinIndexReferenceEmbeddable( pluralAttributeBinding );
					}
					default: {
						return new SqmCollectionIndexReferenceEmbedded( pluralAttributeBinding );
					}
				}
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				switch ( bindingType ) {
					case MAX: {
						return new SqmMaxIndexReferenceEntity( pluralAttributeBinding );
					}
					case MIN: {
						return new SqmMinIndexReferenceEntity( pluralAttributeBinding );
					}
					default: {
						return new SqmCollectionIndexReferenceEntity( pluralAttributeBinding );
					}
				}
			}
			default: {
				throw new NotYetImplementedException(  );
			}
		}
	}

	public static SqmRestrictedCollectionElementReference createIndexedCollectionElementBinding(
			SqmPluralAttributeReference pluralAttributeBinding,
			CollectionElement elementDescriptor,
			SqmExpression selectorExpression) {

		switch ( elementDescriptor.getClassification() ) {
			case BASIC: {
				return new SqmIndexedElementReferenceBasic( pluralAttributeBinding, selectorExpression );
			}
			case EMBEDDABLE: {
				return new SqmIndexedElementReferenceEmbedded( pluralAttributeBinding, selectorExpression );
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				return new SqmIndexedElementReferenceEntity( pluralAttributeBinding, selectorExpression );
			}
			default: {
				throw new NotYetImplementedException();
			}
		}
	}

	public static SqmEntityTypedReference createEntityBinding(EntityValuedExpressableType entityReference) {
		return new SqmEntityReference( entityReference );
	}

	private NavigableBindingHelper() {
	}
}
