/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.internal.BasicSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierComposite;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.ParsingException;
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
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
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

	//  `select p.age from Person p`
	//	`select p.name from Person p`
	//	`select p.address from Person p`
	//	`select p.children from Person p`

	public static SqmNavigableReference createNavigableBinding(
			SqmNavigableContainerReference source,
			Navigable navigable,
			Navigable.SqmReferenceCreationContext creationContext) {
		// the source's `#getExportedFromElement` is always the from-element that "lhs" for any navigable reference -
		// 	it is ultimately the thing used to "qualify" the reference values in the generated SQL.
		//
		// In cases where the navigable is basic or composite valued, this is pretty straight-forward
		//
		// In entity-valued-navigable cases

		//		2)
		//  `select p.age from Person p`
		//		source = the nav-ref for the (Person p) SqmFrom - its #getExportedFrom is the (Person p)
		//		navigable is the Person#age attribute


		//	`select p.name from Person p`


		return navigable.createSqmExpression( source.getExportedFromElement(), source, creationContext );


		//	`select p.address from Person p`
		//	`select p.children from Person p`




//		if ( navigable instanceof EntityIdentifier ) {
//			assert source instanceof SqmEntityTypedReference;
//			return createEntityIdentiferBinding( (SqmEntityTypedReference) source, (EntityIdentifier) navigable );
//		}
//		else if ( navigable instanceof SingularPersistentAttribute ) {
//			return createSingularAttributeBinding( source, (SingularPersistentAttribute) navigable );
//		}
//		else if ( navigable instanceof PluralPersistentAttribute ) {
//			return createPluralAttributeBinding( source, (PluralPersistentAttribute) navigable );
//		}
//		else if ( navigable instanceof CollectionElement ) {
//			return createCollectionElementBinding( source, (CollectionElement) navigable );
//		}
//		else if ( navigable instanceof CollectionIndex ) {
//			return createCollectionIndexBinding( source, (CollectionIndex) navigable );
//		}
//		else if ( navigable instanceof EntityValuedExpressableType ) {
//			// for anything else source should be null
//			assert source == null;
//			return createEntityBinding( (EntityValuedExpressableType) navigable );
//		}
//
//		throw new ParsingException( "Unexpected SqmNavigable for creation of NavigableBinding : " + navigable );
	}

	private static SqmEntityIdentifierReference createEntityIdentiferBinding(
			SqmEntityTypedReference sourceBinding,
			EntityIdentifier navigable) {
		if ( navigable instanceof EntityIdentifierSimple ) {
			return new SqmEntityIdentifierReferenceSimple( sourceBinding, (EntityIdentifierSimple) navigable );
		}
		else {
			return new SqmEntityIdentifierReferenceComposite( sourceBinding, (EntityIdentifierComposite) navigable );
		}
	}

	private static SqmPluralAttributeReference createPluralAttributeBinding(
			SqmNavigableContainerReference lhs,
			PluralPersistentAttribute pluralAttribute) {
		return new SqmPluralAttributeReference( lhs, pluralAttribute );
	}


	public static SqmSingularAttributeReference createSingularAttributeBinding(
			SqmNavigableContainerReference sourceBinding,
			SingularPersistentAttribute attribute) {
		switch ( attribute.getAttributeTypeClassification() ) {
			case BASIC: {
				return new SqmSingularAttributeReferenceBasic(
						sourceBinding,
						(BasicSingularPersistentAttribute) attribute
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
			SqmNavigableContainerReference source,
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
			SqmNavigableContainerReference source,
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
			SqmNavigableContainerReference source,
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
			SqmNavigableContainerReference source,
			CollectionIndex indexDescriptor) {
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

	private NavigableBindingHelper() {
	}
}
