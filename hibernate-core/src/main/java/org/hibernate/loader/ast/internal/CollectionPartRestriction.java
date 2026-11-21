package org.hibernate.loader.ast.internal;

import java.util.List;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.TransientObjectException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ManyToManyCollectionPart;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.from.PluralTableGroup;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Restricts the rows of a collection to those holding a given value of its element
 * or index, in a lookup which does not initialize the collection, and binds the value.
 * An entity is identified by its identifier, which is read from a proxy without
 * initializing it.
 *
 * @see Identification
 */
final class CollectionPartRestriction {

	/**
	 * How a value of an element or index is identified in the rows of the collection,
	 * in order of precedence.
	 */
	private enum Identification {
		/**
		 * An entity which the collection table refers to by a unique key, by its
		 * identifier, in the table group of the entity, joined by the unique key. So
		 * the value of the unique key is read from the database, not from the entity,
		 * which might mean initializing a proxy for it, and might be stale.
		 */
		UNIQUE_KEY_REFERENCE,
		/**
		 * The key of a map mapped by {@code @MapKey}, by the attribute of the element,
		 * in the table group of the element, whose table has its columns.
		 */
		ELEMENT_ATTRIBUTE,
		/**
		 * An entity, by the foreign key referring to it in the collection table, not
		 * by the identifier of the entity, which may also be the element.
		 */
		FOREIGN_KEY,
		/**
		 * The element of a one-to-many collection, by its identifier.
		 */
		OWNING_ENTITY,
		/**
		 * A basic or embeddable value, by its columns.
		 */
		VALUE
	}

	private final CollectionPart part;
	private final Identification identification;
	private final ModelPart restrictedPart;

	private CollectionPartRestriction(CollectionPart part, Identification identification, ModelPart restrictedPart) {
		this.part = part;
		this.identification = identification;
		this.restrictedPart = restrictedPart;
	}

	/**
	 * The restriction to the rows holding a given value of the given element or index.
	 */
	static @Nonnull CollectionPartRestriction of(
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull CollectionPart part) {
		if ( part instanceof ManyToManyCollectionPart manyToMany
				&& !manyToMany.getForeignKeyDescriptor().getTargetPart().isEntityIdentifierMapping() ) {
			return new CollectionPartRestriction( part, Identification.UNIQUE_KEY_REFERENCE,
					manyToMany.getEntityMappingType().getIdentifierMapping() );
		}
		final var elementAttribute = elementAttribute( attributeMapping, part );
		if ( elementAttribute != null ) {
			return new CollectionPartRestriction( part, Identification.ELEMENT_ATTRIBUTE, elementAttribute );
		}
		else if ( part instanceof ManyToManyCollectionPart manyToMany ) {
			return new CollectionPartRestriction( part, Identification.FOREIGN_KEY,
					manyToMany.getForeignKeyDescriptor().getKeyPart() );
		}
		else if ( part instanceof EntityCollectionPart entityPart ) {
			return new CollectionPartRestriction( part, Identification.OWNING_ENTITY,
					entityPart.getEntityMappingType().getIdentifierMapping() );
		}
		else {
			return new CollectionPartRestriction( part, Identification.VALUE, part );
		}
	}

	/**
	 * The attribute of the element entity which is the given index, when it is the key
	 * of a map mapped by {@code @MapKey}, or {@code null}.
	 */
	private static @Nullable AttributeMapping elementAttribute(
			PluralAttributeMapping attributeMapping,
			CollectionPart part) {
		final String propertyName = attributeMapping.getIndexMetadata().getIndexPropertyName();
		return part.getNature() == CollectionPart.Nature.INDEX && propertyName != null
				&& attributeMapping.getElementDescriptor() instanceof EntityCollectionPart element
				? element.getAssociatedEntityMappingType().findAttributeMapping( propertyName )
				: null;
	}

	/**
	 * The number of parameters binding the value.
	 */
	int getJdbcTypeCount() {
		return restrictedPart.getJdbcTypeCount();
	}

	/**
	 * The table group, other than the root, in which the part is restricted: the table
	 * group of an entity referred to by a unique key, or of the element, for an attribute
	 * of the element; or {@code null} if the part is restricted in the root.
	 */
	private @Nullable CollectionPart.Nature joinedTableGroup() {
		return switch ( identification ) {
			case UNIQUE_KEY_REFERENCE -> part.getNature();
			case ELEMENT_ATTRIBUTE -> CollectionPart.Nature.ELEMENT;
			case FOREIGN_KEY, OWNING_ENTITY, VALUE -> null;
		};
	}

	/**
	 * Adds the part to the parts restricted by the select of the rows, unless it is
	 * {@linkplain #restrictJoined restricted in a joined table group} afterward.
	 */
	void addRestrictedPart(@Nonnull List<ModelPart> restrictedParts) {
		if ( joinedTableGroup() == null ) {
			restrictedParts.add( restrictedPart );
		}
	}

	/**
	 * Restricts the part in the {@linkplain #joinedTableGroup joined table group} it
	 * belongs to, once the select of the rows is built.
	 */
	void restrictJoined(@Nonnull QuerySpec querySpec, @Nonnull Consumer<JdbcParameter> jdbcParameterConsumer) {
		final var nature = joinedTableGroup();
		if ( nature != null ) {
			final var tableGroup =
					( (PluralTableGroup) querySpec.getFromClause().getRoots().get( 0 ) )
							.getTableGroup( nature );
			restrictedPart.forEachSelectable( (index, selectable) -> {
				final var jdbcParameter = new SqlTypedMappingJdbcParameter( selectable );
				jdbcParameterConsumer.accept( jdbcParameter );
				final var tableReference =
						tableGroup.resolveTableReference( tableGroup.getNavigablePath(),
								selectable.getContainingTableExpression() );
				querySpec.applyPredicate( new ComparisonPredicate(
						new ColumnReference( tableReference, selectable ),
						ComparisonOperator.EQUAL,
						jdbcParameter
				) );
			} );
		}
	}

	/**
	 * Binds the parameters of the restriction to the given value of the element or index.
	 *
	 * @throws TransientObjectException if the value is an unsaved entity
	 */
	int bind(
			@Nonnull JdbcParameterBindings jdbcParameterBindings,
			@Nullable Object value,
			int offset,
			@Nonnull JdbcParametersList jdbcParameters,
			@Nonnull SharedSessionContractImplementor session) {
		return jdbcParameterBindings.registerParametersForEachJdbcValue(
				identifyingValue( value, session ),
				offset,
				restrictedPart,
				jdbcParameters,
				session
		);
	}

	private @Nullable Object identifyingValue(@Nullable Object value, SharedSessionContractImplementor session) {
		return switch ( identification ) {
			case UNIQUE_KEY_REFERENCE, OWNING_ENTITY -> identifier( (EntityIdentifierMapping) restrictedPart, value, session );
			case FOREIGN_KEY -> {
				final var foreignKey = ( (ManyToManyCollectionPart) part ).getForeignKeyDescriptor();
				yield foreignKey.getAssociationKeyFromSide( value, foreignKey.getTargetSide(), session );
			}
			case ELEMENT_ATTRIBUTE, VALUE -> value;
		};
	}

	private static @Nullable Object identifier(
			EntityIdentifierMapping identifierMapping,
			@Nullable Object entity,
			SharedSessionContractImplementor session) {
		final var lazyInitializer = extractLazyInitializer( entity );
		return lazyInitializer != null
				? lazyInitializer.getInternalIdentifier()
				: identifierMapping.getIdentifierIfNotUnsaved( entity, session );
	}
}
