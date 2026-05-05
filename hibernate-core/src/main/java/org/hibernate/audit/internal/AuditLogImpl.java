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
 * audit tables using HQL with {@code transactionId()} and
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
	private final @Nullable String revisionEntityName;
	private final @Nullable String transactionIdProperty;
	private final @Nullable String timestampProperty;
	private final @Nullable String modifiedEntitiesProperty;
	private final @Nullable Class<?> timestampFieldType;

	/**
	 * @param auditSession a session configured with {@link AuditLog#ALL_REVISIONS}
	 * temporal context for reading audit tables
	 */
	public AuditLogImpl(SharedSessionContractImplementor auditSession) {
		this.auditSession = auditSession;
		this.sessionFactory = auditSession.getSessionFactory();
		final var supplier = ChangesetEntitySupplier.resolve( sessionFactory.getServiceRegistry() );
		if ( supplier != null ) {
			this.changesetEntitySupplier = supplier;
			this.revisionEntityName = sessionFactory.getMappingMetamodel()
					.getEntityDescriptor( supplier.getRevisionEntityClass() )
					.getEntityName();
			this.transactionIdProperty = supplier.getChangesetIdProperty();
			this.timestampProperty = supplier.getTimestampProperty();
			this.modifiedEntitiesProperty = supplier.getModifiedEntitiesProperty();
			this.timestampFieldType = sessionFactory.getMappingMetamodel()
					.getEntityDescriptor( supplier.getRevisionEntityClass() )
					.findAttributeMapping( supplier.getTimestampProperty() )
					.getJavaType().getJavaTypeClass();
		}
		else {
			this.modifiedEntitiesProperty = null;
			this.changesetEntitySupplier = null;
			this.revisionEntityName = null;
			this.transactionIdProperty = null;
			this.timestampProperty = null;
			this.timestampFieldType = null;
		}
	}

	@Override
	public List<Object> getRevisions(Class<?> entityClass, Object id) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( id, "Primary key" );
		final var entityName = requireAuditedEntityName( entityClass );
		return auditSession.createSelectionQuery(
				"select transactionId(e) from " + entityName + " e"
						+ " where e.id = :id"
						+ " order by transactionId(e)",
				Object.class
		).setParameter( "id", id ).getResultList();
	}

	@Override
	public ModificationType getModificationType(Class<?> entityClass, Object id, Object transactionId) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( id, "Primary key" );
		requireNonNull( transactionId, "Transaction identifier" );
		final var entityName = requireAuditedEntityName( entityClass );
		return auditSession.createSelectionQuery(
						"select modificationType(e) from " + entityName + " e"
								+ " where e.id = :id"
								+ " and transactionId(e) = :txId",
						ModificationType.class
				).setParameter( "id", id )
				.setParameter( "txId", transactionId )
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
	public <T> T find(Class<T> entityClass, Object id, Object transactionId) {
		return find( entityClass, id, transactionId, false );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object id, Object transactionId, boolean includeDeletions) {
		requireNonNull( entityClass, "Entity class" );
		return doFind( requireAuditedEntityName( entityClass ), id, transactionId, includeDeletions );
	}

	private <T> T doFind(String entityName, Object id, Object transactionId, boolean includeDeletions) {
		requireNonNull( id, "Primary key" );
		requireNonNull( transactionId, "Transaction identifier" );
		final var persister = sessionFactory.getMappingMetamodel().getEntityDescriptor( entityName );
		return persister.getAuditMapping()
				.getEntityLoader()
				.find( id, transactionId, includeDeletions, auditSession );
	}

	@Override
	public <T> T find(Class<T> entityClass, Object id, Instant instant) {
		return find( entityClass, id, getTransactionId( instant ) );
	}

	@Override
	public <T> List<T> findEntitiesModifiedAt(Class<T> entityClass, Object transactionId) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( transactionId, "Transaction identifier" );
		return doFindEntitiesModifiedAt(
				requireAuditedEntityName( entityClass ),
				transactionId,
				null,
				entityClass
		);
	}

	@Override
	public <T> List<T> findEntitiesModifiedAt(
			Class<T> entityClass,
			Object transactionId,
			ModificationType modificationType) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( transactionId, "Transaction identifier" );
		requireNonNull( modificationType, "Modification type" );
		return doFindEntitiesModifiedAt(
				requireAuditedEntityName( entityClass ),
				transactionId,
				modificationType,
				entityClass
		);
	}

	@Override
	public <T> Map<ModificationType, List<T>> findEntitiesGroupedByModificationType(
			Class<T> entityClass,
			Object transactionId) {
		requireNonNull( entityClass, "Entity class" );
		requireNonNull( transactionId, "Transaction identifier" );
		return doFindEntitiesGroupedByModificationType(
				requireAuditedEntityName( entityClass ),
				transactionId,
				entityClass
		);
	}

	private <T> List<T> doFindEntitiesModifiedAt(
			String entityName,
			Object transactionId,
			@Nullable ModificationType modificationType,
			Class<T> resultType) {
		var hql = "from " + entityName + " e where transactionId(e) = :txId";
		if ( modificationType != null ) {
			hql += " and modificationType(e) = :modType";
		}
		final var query = auditSession
				.createSelectionQuery( hql, resultType )
				.setParameter( "txId", transactionId );
		if ( modificationType != null ) {
			query.setParameter( "modType", modificationType );
		}
		return query.getResultList();
	}

	@SuppressWarnings("unchecked")
	private <T> Map<ModificationType, List<T>> doFindEntitiesGroupedByModificationType(
			String entityName, Object transactionId, Class<T> resultType) {
		final var values = ModificationType.values();
		final Map<ModificationType, List<T>> result = new HashMap<>( values.length );
		for ( ModificationType mt : values ) {
			result.put( mt, new ArrayList<>() );
		}
		final List<Object[]> rows = auditSession.createSelectionQuery(
				"select e, modificationType(e) from " + entityName + " e"
						+ " where transactionId(e) = :txId",
				Object[].class
		).setParameter( "txId", transactionId ).getResultList();
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
		if ( revisionEntityName != null ) {
			hql = "select e, r, modificationType(e)"
					+ " from " + entityName + " e"
					+ " join " + revisionEntityName + " r"
					+ " on r." + transactionIdProperty + " = transactionId(e)"
					+ " where e.id = :id"
					+ " order by transactionId(e)";
		}
		else {
			hql = "select e, transactionId(e), modificationType(e)"
					+ " from " + entityName + " e"
					+ " where e.id = :id"
					+ " order by transactionId(e)";
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

	// --- Cross-type revision queries ---

	@Override
	public Set<Class<?>> getEntityTypesModifiedAt(Object transactionId) {
		requireNonNull( transactionId, "Transaction identifier" );
		requireEntityChangeTracking();
		final var entityNames = queryRevChangesEntityNames( transactionId );
		final Set<Class<?>> result = new HashSet<>();
		for ( String entityName : entityNames ) {
			result.add( sessionFactory.getMappingMetamodel().getEntityDescriptor( entityName ).getMappedClass() );
		}
		return result;
	}

	@Override
	public List<Object> findAllEntitiesModifiedAt(Object transactionId) {
		requireNonNull( transactionId, "Transaction identifier" );
		requireEntityChangeTracking();
		final var entityNames = queryRevChangesEntityNames( transactionId );
		final List<Object> result = new ArrayList<>();
		for ( String entityName : entityNames ) {
			result.addAll( doFindEntitiesModifiedAt( entityName, transactionId, null, Object.class ) );
		}
		return result;
	}

	@Override
	public List<Object> findAllEntitiesModifiedAt(Object transactionId, ModificationType modificationType) {
		requireNonNull( transactionId, "Transaction identifier" );
		requireNonNull( modificationType, "Modification type" );
		requireEntityChangeTracking();
		final var entityNames = queryRevChangesEntityNames( transactionId );
		final List<Object> result = new ArrayList<>();
		for ( String entityName : entityNames ) {
			result.addAll( doFindEntitiesModifiedAt( entityName, transactionId, modificationType, Object.class ) );
		}
		return result;
	}

	@Override
	public Map<ModificationType, List<Object>> findAllEntitiesGroupedByModificationType(Object transactionId) {
		requireNonNull( transactionId, "Transaction identifier" );
		requireEntityChangeTracking();
		final var values = ModificationType.values();
		final Map<ModificationType, List<Object>> result = new HashMap<>( values.length );
		for ( ModificationType mt : values ) {
			result.put( mt, new ArrayList<>() );
		}
		for ( String entityName : queryRevChangesEntityNames( transactionId ) ) {
			doFindEntitiesGroupedByModificationType( entityName, transactionId, Object.class )
					.forEach( (mt, entities) -> result.get( mt ).addAll( entities ) );
		}
		return result;
	}

	private List<String> queryRevChangesEntityNames(Object transactionId) {
		return auditSession.createSelectionQuery(
				"select element(r." + modifiedEntitiesProperty + ")"
						+ " from " + revisionEntityName + " r"
						+ " where r." + transactionIdProperty + " = :txId",
				String.class
		).setParameter( "txId", transactionId ).getResultList();
	}

	private void requireEntityChangeTracking() {
		if ( modifiedEntitiesProperty == null ) {
			throw new AuditException(
					"Entity change tracking is not enabled. "
							+ "Use a @RevisionEntity with a @RevisionEntity.ModifiedEntities property "
							+ "(e.g. DefaultTrackingModifiedEntitiesRevisionEntity)."
			);
		}
	}

	// --- Revision entity queries ---

	@Override
	public Instant getTransactionTimestamp(Object transactionId) {
		requireNonNull( transactionId, "Transaction identifier" );
		requireRevisionEntity();
		final String hql = "select e." + timestampProperty
				+ " from " + revisionEntityName + " e"
				+ " where e." + transactionIdProperty + " = :rev";
		final var result = auditSession
				.createSelectionQuery( hql, Object.class )
				.setParameter( "rev", transactionId )
				.getSingleResultOrNull();
		if ( result == null ) {
			throw new AuditException( "Revision does not exist: " + transactionId );
		}
		return toInstant( result );
	}

	@Override
	public Object getTransactionId(Instant instant) {
		requireNonNull( instant, "Instant" );
		return resolveTransactionIdForTimestamp( resolveTimestampValue( instant ) );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T findRevision(Object transactionId) {
		requireRevisionEntity();
		final var result = auditSession.createSelectionQuery(
				"from " + revisionEntityName + " where " + transactionIdProperty + " = :rev",
				Object.class
		).setParameter( "rev", transactionId ).getSingleResultOrNull();
		if ( result == null ) {
			throw new AuditException( "Revision does not exist: " + transactionId );
		}
		return (T) result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<Object, T> findRevisions(Set<?> transactionIds) {
		requireRevisionEntity();
		final var results = auditSession.createSelectionQuery(
				"select r." + transactionIdProperty + ", r"
						+ " from " + revisionEntityName + " r"
						+ " where r." + transactionIdProperty + " in :revs"
						+ " order by r." + transactionIdProperty,
				Object[].class
		).setParameter( "revs", transactionIds ).getResultList();
		final Map<Object, T> map = new LinkedHashMap<>();
		for ( var row : results ) {
			//noinspection unchecked
			map.put( row[0], (T) row[1] );
		}
		return map;
	}

	// --- Helpers ---

	private Object resolveTransactionIdForTimestamp(Object timestampValue) {
		requireRevisionEntity();
		final String hql = "select max(e." + transactionIdProperty + ")"
				+ " from " + revisionEntityName + " e"
				+ " where e." + timestampProperty + " <= :ts";
		final var result = auditSession
				.createSelectionQuery( hql, Object.class )
				.setParameter( "ts", timestampValue )
				.getSingleResultOrNull();
		if ( result == null ) {
			throw new AuditException( "No revision exists at or before the given date" );
		}
		return result;
	}

	/**
	 * Convert an {@link Instant} to match the revision entity's
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

	private void requireRevisionEntity() {
		if ( changesetEntitySupplier == null ) {
			throw new AuditException(
					"No @RevisionEntity configured. "
							+ "This operation requires a revision entity with "
							+ "@RevisionEntity.TransactionId and @RevisionEntity.Timestamp fields."
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
		throw new AuditException( "Cannot convert revision timestamp to Instant: " + value );
	}
}
