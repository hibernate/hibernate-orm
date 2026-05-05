/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit.internal;

import org.checkerframework.checker.nullness.qual.Nullable;

import org.hibernate.audit.AuditEntry;
import org.hibernate.audit.AuditException;
import org.hibernate.audit.AuditLog;
import org.hibernate.audit.ModificationType;
import org.hibernate.audit.spi.ChangesetEntitySupplier;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Session-scoped implementation of {@link AuditLog} that queries
 * audit tables using HQL with {@code changesetId()} and
 * {@code modificationType()} functions.
 * <p>
 * Obtained via {@link org.hibernate.audit.AuditLogFactory#create}.
 * Uses an internal session with {@code ALL_REVISIONS} temporal
 * context for querying audit tables.
 *
 * @author Marco Belladelli
 * @since 7.4
 */
public class AuditLogImpl implements AuditLog {
	private final SessionFactoryImplementor sessionFactory;
	private final SharedSessionContractImplementor auditSession;
	private final @Nullable ChangesetEntitySupplier<?> changesetEntitySupplier;
	private final @Nullable String changesetEntityName;
	private final @Nullable String changesetIdProperty;
	private final @Nullable String timestampProperty;
	private final @Nullable String modifiedEntitiesProperty;
	private final @Nullable Class<?> timestampFieldType;

	/**
	 * @param auditSession a session configured with {@link AuditLog#ALL_CHANGESETS}
	 * temporal context for reading audit tables
	 */
	public AuditLogImpl(SharedSessionContractImplementor auditSession) {
		this.auditSession = auditSession;
		this.sessionFactory = auditSession.getSessionFactory();
		final var supplier = ChangesetEntitySupplier.resolve( sessionFactory.getServiceRegistry() );
		if ( supplier != null ) {
			this.changesetEntitySupplier = supplier;
			this.changesetEntityName = sessionFactory.getMappingMetamodel()
					.getEntityDescriptor( supplier.getChangesetEntityClass() )
					.getEntityName();
			this.changesetIdProperty = supplier.getChangesetIdProperty();
			this.timestampProperty = supplier.getTimestampProperty();
			this.modifiedEntitiesProperty = supplier.getModifiedEntitiesProperty();
			this.timestampFieldType = sessionFactory.getMappingMetamodel()
					.getEntityDescriptor( supplier.getChangesetEntityClass() )
					.findAttributeMapping( supplier.getTimestampProperty() )
					.getJavaType().getJavaTypeClass();
		}
		else {
			this.modifiedEntitiesProperty = null;
			this.changesetEntitySupplier = null;
			this.changesetEntityName = null;
			this.changesetIdProperty = null;
			this.timestampProperty = null;
			this.timestampFieldType = null;
		}
	}

	@Override
	public List<Object> getChangesets(Class<?> entityClass, Object id) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( id, "Primary key" );
		final var entityName = requireAuditedEntityName( entityClass );
		return auditSession.createSelectionQuery(
				"select changesetId(e) from " + entityName + " e"
						+ " where e.id = :id"
						+ " order by changesetId(e)",
				Object.class
		).setParameter( "id", id ).getResultList();
	}

	@Override
	public ModificationType getModificationType(Class<?> entityClass, Object id, Object changesetId) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( id, "Primary key" );
		requireNonNull( changesetId, "Changeset identifier" );
		final var entityName = requireAuditedEntityName( entityClass );
		return auditSession.createSelectionQuery(
						"select modificationType(e) from " + entityName + " e"
								+ " where e.id = :id"
								+ " and changesetId(e) = :csId",
						ModificationType.class
				).setParameter( "id", id )
				.setParameter( "csId", changesetId )
				.getSingleResultOrNull();
	}

	@Override
	public boolean isAudited(Class<?> entityClass) {
		requireNonNull( entityClass, "Entity class" );
		return sessionFactory.getMappingMetamodel()
				.getEntityDescriptor( entityClass )
				.getAuditMapping() != null;
	}

	@Override
	public <T> T find(Class<T> entityClass, Object id, Object changesetId) {
		return find( entityClass, id, changesetId, false );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object id, Object changesetId, boolean includeDeletions) {
		requireNonNull( entityClass, "Entity class" );
		return doFind( requireAuditedEntityName( entityClass ), id, changesetId, includeDeletions );
	}

	private <T> T doFind(String entityName, Object id, Object changesetId, boolean includeDeletions) {
		requireNonNull( id, "Primary key" );
		requireNonNull( changesetId, "Changeset identifier" );
		final var persister = sessionFactory.getMappingMetamodel().getEntityDescriptor( entityName );
		return persister.getAuditMapping()
				.getEntityLoader()
				.find( id, changesetId, includeDeletions, auditSession );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object id, Instant instant) {
		return find( entityClass, id, getChangesetId( instant ) );
	}

	@Override
	public <T> List<T> findEntitiesModifiedAt(Class<T> entityClass, Object changesetId) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( changesetId, "Changeset identifier" );
		return doFindEntitiesModifiedAt(
				requireAuditedEntityName( entityClass ),
				changesetId,
				null,
				entityClass
		);
	}

	@Override
	public <T> List<T> findEntitiesModifiedAt(
			Class<T> entityClass,
			Object changesetId,
			ModificationType modificationType) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( changesetId, "Changeset identifier" );
		requireNonNull( modificationType, "Modification type" );
		return doFindEntitiesModifiedAt(
				requireAuditedEntityName( entityClass ),
				changesetId,
				modificationType,
				entityClass
		);
	}

	@Override
	public <T> Map<ModificationType, List<T>> findEntitiesGroupedByModificationType(
			Class<T> entityClass,
			Object changesetId) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( changesetId, "Changeset identifier" );
		return doFindEntitiesGroupedByModificationType(
				requireAuditedEntityName( entityClass ),
				changesetId,
				entityClass
		);
	}

	private <T> List<T> doFindEntitiesModifiedAt(
			String entityName,
			Object changesetId,
			@Nullable ModificationType modificationType,
			Class<T> resultType) {
		var hql = "from " + entityName + " e where changesetId(e) = :csId";
		if ( modificationType != null ) {
			hql += " and modificationType(e) = :modType";
		}
		final var query = auditSession
				.createSelectionQuery( hql, resultType )
				.setParameter( "csId", changesetId );
		if ( modificationType != null ) {
			query.setParameter( "modType", modificationType );
		}
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	private <T> Map<ModificationType, List<T>> doFindEntitiesGroupedByModificationType(
			String entityName, Object changesetId, Class<T> resultType) {
		final var values = ModificationType.values();
		final Map<ModificationType, List<T>> result = new HashMap<>( values.length );
		for ( ModificationType mt : values ) {
			result.put( mt, new ArrayList<>() );
		}
		final List<Object[]> rows = auditSession.createSelectionQuery(
				"select e, modificationType(e) from " + entityName + " e"
						+ " where changesetId(e) = :csId",
				Object[].class
		).setParameter( "csId", changesetId ).getResultList();
		for ( var row : rows ) {
			result.get( (ModificationType) row[1] ).add( (T) row[0] );
		}
		return result;
	}

	@Override
	public <T> List<AuditEntry<T>> getHistory(Class<T> entityClass, Object id) {
		requireNonNull( entityClass, "Entity class" );
		return doGetHistory( requireAuditedEntityName( entityClass ), id );
	}

	private <T> List<AuditEntry<T>> doGetHistory(String entityName, Object id) {
		requireNonNull( id, "Primary key" );

		final String hql;
		if ( changesetEntityName != null ) {
			hql = "select e, r, modificationType(e)"
					+ " from " + entityName + " e"
					+ " join " + changesetEntityName + " r"
					+ " on r." + changesetIdProperty + " = changesetId(e)"
					+ " where e.id = :id"
					+ " order by changesetId(e)";
		}
		else {
			hql = "select e, changesetId(e), modificationType(e)"
					+ " from " + entityName + " e"
					+ " where e.id = :id"
					+ " order by changesetId(e)";
		}

		final List<Object[]> rows = auditSession
				.createSelectionQuery( hql, Object[].class )
				.setParameter( "id", id ).getResultList();

		final List<AuditEntry<T>> result = new ArrayList<>( rows.size() );
		for ( var row : rows ) {
			//noinspection unchecked
			final var entity = (T) row[0];
			result.add( new AuditEntry<>( entity, row[1], (ModificationType) row[2] ) );
		}
		return result;
	}

	// --- Cross-type changeset queries ---

	@Override
	public Set<Class<?>> getEntityTypesModifiedAt(Object changesetId) {
		requireNonNull( changesetId, "Changeset identifier" );
		requireEntityChangeTracking();
		final var entityNames = queryRevChangesEntityNames( changesetId );
		final Set<Class<?>> result = new HashSet<>();
		for ( String entityName : entityNames ) {
			result.add( sessionFactory.getMappingMetamodel().getEntityDescriptor( entityName ).getMappedClass() );
		}
		return result;
	}

	@Override
	public List<Object> findAllEntitiesModifiedAt(Object changesetId) {
		requireNonNull( changesetId, "Changeset identifier" );
		requireEntityChangeTracking();
		final var entityNames = queryRevChangesEntityNames( changesetId );
		final List<Object> result = new ArrayList<>();
		for ( String entityName : entityNames ) {
			result.addAll( doFindEntitiesModifiedAt( entityName, changesetId, null, Object.class ) );
		}
		return result;
	}

	@Override
	public List<Object> findAllEntitiesModifiedAt(Object changesetId, ModificationType modificationType) {
		requireNonNull( changesetId, "Changeset identifier" );
		requireNonNull( modificationType, "Modification type" );
		requireEntityChangeTracking();
		final var entityNames = queryRevChangesEntityNames( changesetId );
		final List<Object> result = new ArrayList<>();
		for ( String entityName : entityNames ) {
			result.addAll( doFindEntitiesModifiedAt( entityName, changesetId, modificationType, Object.class ) );
		}
		return result;
	}

	@Override
	public Map<ModificationType, List<Object>> findAllEntitiesGroupedByModificationType(Object changesetId) {
		requireNonNull( changesetId, "Changeset identifier" );
		requireEntityChangeTracking();
		final var values = ModificationType.values();
		final Map<ModificationType, List<Object>> result = new HashMap<>( values.length );
		for ( ModificationType mt : values ) {
			result.put( mt, new ArrayList<>() );
		}
		for ( String entityName : queryRevChangesEntityNames( changesetId ) ) {
			doFindEntitiesGroupedByModificationType( entityName, changesetId, Object.class )
					.forEach( (mt, entities) -> result.get( mt ).addAll( entities ) );
		}
		return result;
	}

	private List<String> queryRevChangesEntityNames(Object changesetId) {
		return auditSession.createSelectionQuery(
				"select element(r." + modifiedEntitiesProperty + ")"
						+ " from " + changesetEntityName + " r"
						+ " where r." + changesetIdProperty + " = :csId",
				String.class
		).setParameter( "csId", changesetId ).getResultList();
	}

	private void requireEntityChangeTracking() {
		if ( modifiedEntitiesProperty == null ) {
			throw new AuditException(
					"Entity change tracking is not enabled. "
							+ "Use a @ChangesetEntity with a @ChangesetEntity.ModifiedEntities property "
							+ "(e.g. DefaultTrackingModifiedEntitiesChangesetEntity)."
			);
		}
	}

	// --- Changeset entity queries ---

	@Override
	public Instant getChangesetTimestamp(Object changesetId) {
		requireNonNull( changesetId, "Changeset identifier" );
		requireChangesetEntity();
		final String hql = "select e." + timestampProperty
				+ " from " + changesetEntityName + " e"
				+ " where e." + changesetIdProperty + " = :rev";
		final var result = auditSession
				.createSelectionQuery( hql, Object.class )
				.setParameter( "rev", changesetId )
				.getSingleResultOrNull();
		if ( result == null ) {
			throw new AuditException( "Changeset does not exist: " + changesetId );
		}
		return toInstant( result );
	}

	@Override
	public Object getChangesetId(Instant instant) {
		requireNonNull( instant, "Instant" );
		return resolveChangesetIdForTimestamp( resolveTimestampValue( instant ) );
	}

	@Override
	public <T> T findChangeset(Class<T> changesetEntityClass, Object changesetId) {
		requireChangesetEntity();
		final var result = auditSession.createSelectionQuery(
				"from " + changesetEntityName + " where " + changesetIdProperty + " = :rev",
				changesetEntityClass
		).setParameter( "rev", changesetId ).getSingleResultOrNull();
		if ( result == null ) {
			throw new AuditException( "Changeset does not exist: " + changesetId );
		}
		return result;
	}

	@Override
	public <T> Map<Object, T> findChangesets(Class<T> changesetEntityClass, Set<?> changesetIds) {
		requireChangesetEntity();
		final var results = auditSession.createSelectionQuery(
				"select r." + changesetIdProperty + ", r"
						+ " from " + changesetEntityName + " r"
						+ " where r." + changesetIdProperty + " in :revs"
						+ " order by r." + changesetIdProperty,
				Object[].class
		).setParameter( "revs", changesetIds ).getResultList();
		final Map<Object, T> map = new LinkedHashMap<>();
		for ( var row : results ) {
			map.put( row[0], changesetEntityClass.cast( row[1] ) );
		}
		return map;
	}

	// --- Helpers ---

	private Object resolveChangesetIdForTimestamp(Object timestampValue) {
		requireChangesetEntity();
		final String hql = "select max(e." + changesetIdProperty + ")"
				+ " from " + changesetEntityName + " e"
				+ " where e." + timestampProperty + " <= :ts";
		final var result = auditSession
				.createSelectionQuery( hql, Object.class )
				.setParameter( "ts", timestampValue )
				.getSingleResultOrNull();
		if ( result == null ) {
			throw new AuditException( "No changeset exists at or before the given date" );
		}
		return result;
	}

	/**
	 * Convert an {@link Instant} to match the changeset entity's
	 * timestamp field type.
	 */
	private Object resolveTimestampValue(Instant instant) {
		if ( timestampFieldType == Date.class ) {
			return Date.from( instant );
		}
		else if ( timestampFieldType == LocalDateTime.class ) {
			return LocalDateTime.ofInstant( instant, ZoneId.systemDefault() );
		}
		else if ( timestampFieldType == Instant.class ) {
			return instant;
		}
		else {
			return instant.toEpochMilli();
		}
	}

	private void requireChangesetEntity() {
		if ( changesetEntitySupplier == null ) {
			throw new AuditException(
					"No @ChangesetEntity configured. "
							+ "This operation requires a changeset entity with "
							+ "@ChangesetEntity.ChangesetId and @ChangesetEntity.Timestamp fields."
			);
		}
	}

	@Override
	public void close() {
		if ( auditSession.isOpen() ) {
			auditSession.close();
		}
	}

	private String requireAuditedEntityName(Class<?> entityClass) {
		final var persister = sessionFactory.getMappingMetamodel().getEntityDescriptor( entityClass );
		if ( persister.getAuditMapping() == null ) {
			throw new IllegalArgumentException(
					"Entity '" + persister.getEntityName() + "' is not audited"
			);
		}
		return persister.getEntityName();
	}

	private static Instant toInstant(Object value) {
		if ( value instanceof Instant instant ) {
			return instant;
		}
		else if ( value instanceof LocalDateTime localDateTime ) {
			return localDateTime.atZone( ZoneId.systemDefault() ).toInstant();
		}
		else if ( value instanceof Date date ) {
			return date.toInstant();
		}
		else if ( value instanceof Long millis ) {
			return Instant.ofEpochMilli( millis );
		}
		throw new AuditException( "Cannot convert changeset timestamp to Instant: " + value );
	}
}
