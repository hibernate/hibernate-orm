package org.hibernate.action.queue.spi.bind;

import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.internal.GeneratedValuesImpl;
import org.hibernate.generator.values.internal.GeneratedValuesMappingProducer;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getActualGeneratedModelParts;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.noCustomSql;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/// Used from [org.hibernate.action.queue.internal.decompose.entity.EntityInsertBindPlan]
/// and [org.hibernate.action.queue.internal.decompose.entity.EntityUpdateBindPlan] to aggregate
/// generated value collection across all tables.
///
/// @see org.hibernate.action.queue.internal.decompose.entity.PostInsertHandling
/// @see org.hibernate.action.queue.internal.decompose.entity.PostUpdateHandling
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating(since = "8.0", group = "action-queue")
public final class GeneratedValuesCollector {
	public static @Nullable GeneratedValuesCollector forInsert(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		return forParts( entityPersister, EventType.INSERT, generatedInsertParts( entityPersister, sessionFactory ) );
	}

	private static List<? extends ModelPart> generatedInsertParts(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		final var dialect = sessionFactory.getJdbcServices().getDialect();
		final var generatedValuesSupport = dialect.getGeneratedValuesSupport();
		final var supportsRowId = generatedValuesSupport.supports( GeneratedValuesSupport.Capability.INSERT_RETURNING )
				&& generatedValuesSupport.supports( GeneratedValuesSupport.Capability.INSERT_RETURNING_ROW_ID )
				&& noCustomSql( entityPersister, EventType.INSERT );
		return getActualGeneratedModelParts( entityPersister, EventType.INSERT, true, supportsRowId );
	}

	/// A temporal replacement binds the existing identifier; only other values may be generated.
	public static @Nullable GeneratedValuesCollector forReplacementInsert(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		final var delegate = entityPersister.getInsertDelegate();
		if ( delegate == null || !delegate.supportsArbitraryValues() ) {
			return null;
		}
		final var generatedModelParts = generatedInsertParts( entityPersister, sessionFactory ).stream()
				.filter( part -> !part.isEntityIdentifierMapping() )
				.toList();
		return forParts( entityPersister, EventType.INSERT, generatedModelParts );
	}

	public static @Nullable GeneratedValuesCollector forUpdate(
			EntityPersister entityPersister,
			SessionFactoryImplementor sessionFactory) {
		final var updateDelegate = entityPersister.getUpdateDelegate();
		if ( updateDelegate != null
				&& updateDelegate.getGeneratedValuesMappingProducer()
						instanceof GeneratedValuesMappingProducer mappingProducer ) {
			final List<BasicValuedModelPart> list = new ArrayList<>();
			for ( var resultBuilder : mappingProducer.getResultBuilders() ) {
				list.add( resultBuilder.getModelPart() );
			}
			return forParts( entityPersister, EventType.UPDATE, list );
		}
		return forTiming( entityPersister, EventType.UPDATE, false );
	}

	public static @Nullable GeneratedValuesCollector forTiming(
			EntityPersister entityPersister,
			EventType timing,
			boolean supportsRowId) {
		return forParts( entityPersister, timing, getActualGeneratedModelParts( entityPersister, timing, true, supportsRowId ) );
	}

	private static @Nullable GeneratedValuesCollector forParts(
			EntityPersister entityPersister,
			EventType timing,
			List<? extends ModelPart> generatedModelParts) {
		return isEmpty( generatedModelParts )
				? null
				: new GeneratedValuesCollector( timing, entityPersister, generatedModelParts );
	}

	private final EventType timing;
	private final EntityPersister entityPersister;
	private final List<? extends ModelPart> generatedModelParts;
	private final GeneratedValues generatedValues;
	private DelayedValueAccess identifierHandle;

	public GeneratedValuesCollector(
			EventType timing,
			EntityPersister entityPersister,
			List<? extends ModelPart> generatedModelParts) {
		this.timing = timing;
		this.entityPersister = entityPersister;
		this.generatedModelParts = generatedModelParts;
		this.generatedValues = new GeneratedValuesImpl( generatedModelParts );
	}

	public EventType getTiming() {
		return timing;
	}

	public EntityPersister getEntityPersister() {
		return entityPersister;
	}

	public void apply(GeneratedValues generatedValues) {
		this.generatedValues.apply( generatedValues );
	}

	public GeneratedValues generatedValues() {
		return generatedValues;
	}

	public boolean containsGeneratedValues(TableDescriptor tableDescriptor) {
		for ( var generatedModelPart : generatedModelParts ) {
			final BasicValuedModelPart basicValuedModelPart = generatedModelPart.asBasicValuedModelPart();
			if ( basicValuedModelPart != null
					&& tableDescriptor.name().equals( basicValuedModelPart.getContainingTableExpression() ) ) {
				return true;
			}
		}
		return false;
	}

	public void setIdentifierHandle(DelayedValueAccess identifierHandle) {
		this.identifierHandle = identifierHandle;
	}

	public DelayedValueAccess getIdentifierHandle() {
		return identifierHandle;
	}

	@Override
	public String toString() {
		return String.format( Locale.ROOT,
				"GeneratedValuesCollector(%s:%s)",
				timing.name(),
				entityPersister.getEntityName()
		);
	}


}
