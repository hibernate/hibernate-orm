package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.AggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * Support for {@link jakarta.persistence.EmbeddedId}
 *
 * @author Andrea Boriero
 */
public class EmbeddedIdentifierMappingImpl
		extends AbstractCompositeIdentifierMapping
		implements AggregatedIdentifierMapping {
	private final String name;
	private final EmbeddableMappingType embeddableDescriptor;
	private final PropertyAccess propertyAccess;

	public EmbeddedIdentifierMappingImpl(
			EntityMappingType entityMapping,
			String name,
			EmbeddableMappingType embeddableDescriptor,
			PropertyAccess propertyAccess,
			String tableExpression,
			MappingModelCreationProcess creationProcess) {
		super( entityMapping, tableExpression, creationProcess );

		this.name = name;
		this.embeddableDescriptor = embeddableDescriptor;
		this.propertyAccess = propertyAccess;
	}

	@Nonnull
	@Override
	public String getPartName() {
		return name;
	}

	@Nonnull
	@Override
	public Nature getNature() {
		return Nature.COMPOSITE;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getPartMappingType() {
		return embeddableDescriptor;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor() {
		return getMappedType();
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath, @Nonnull TableGroup tableGroup, @Nonnull DomainResultCreationState creationState) {
		getEmbeddableTypeDescriptor().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		getEmbeddableTypeDescriptor()
				.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Nullable
	@Override
	public Object getIdentifier(@Nonnull Object entity) {
		final var lazyInitializer = extractLazyInitializer( entity );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getInternalIdentifier();
		}
		return propertyAccess.getPropertyValueAccessor().get( entity );
	}

	@Override
	public void setIdentifier(@Nonnull Object entity, @Nullable Object id, @Nonnull SharedSessionContractImplementor session) {
		propertyAccess.getPropertyValueAccessor().set( entity, id );
	}

	@Override
	public String getSqlAliasStem() {
		return name;
	}


	@Override
	public String getFetchableName() {
		return name;
	}

	@Nonnull
	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Nonnull
	@Override
	public String getAttributeName() {
		return name;
	}

	@Override
	public int compare(@Nullable Object value1, @Nullable Object value2) {
		return getEmbeddableTypeDescriptor().compare( value1, value2 );
	}
}
