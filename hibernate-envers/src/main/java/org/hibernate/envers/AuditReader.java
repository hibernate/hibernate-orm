/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.exception.RevisionDoesNotExistException;
import org.hibernate.envers.query.AuditQueryCreator;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacute;n Chanfreau
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public interface AuditReader {
	/**
	 * Find an entity by primary key at the given revision.
	 *
	 * @param cls Class of the entity.
	 * @param primaryKey Primary key of the entity.
	 * @param revision Revision in which to get the entity.
	 * @param <T> The type of the entity to find
	 *
	 * @return The found entity instance at the given revision (its properties may be partially filled
	 *         if not all properties are audited) or null, if an entity with that id didn't exist at that
	 *         revision.
	 *
	 * @throws IllegalArgumentException If cls or primaryKey is null or revision is less or equal to 0.
	 * @throws NotAuditedException When entities of the given class are not audited.
	 * @throws IllegalStateException If the associated entity manager is closed.
	 */
	<T> T find(Class<T> cls, Object primaryKey, Number revision) throws
			IllegalArgumentException, NotAuditedException, IllegalStateException;

	/**
	 * Find an entity by primary key at the given revision with the specified entityName.
	 *
	 * @param cls Class of the entity.
	 * @param entityName Name of the entity (if can't be guessed basing on the {@code cls}).
	 * @param primaryKey Primary key of the entity.
	 * @param revision Revision in which to get the entity.
	 * @param <T> The type of the entity to find
	 *
	 * @return The found entity instance at the given revision (its properties may be partially filled
	 *         if not all properties are audited) or null, if an entity with that id didn't exist at that
	 *         revision.
	 *
	 * @throws IllegalArgumentException If cls or primaryKey is null or revision is less or equal to 0.
	 * @throws NotAuditedException When entities of the given class are not audited.
	 * @throws IllegalStateException If the associated entity manager is closed.
	 */
	<T> T find(
			Class<T> cls, String entityName, Object primaryKey,
			Number revision) throws IllegalArgumentException,
			NotAuditedException, IllegalStateException;

	/**
	 * Find an entity by primary key at the given revision with the specified entityName,
	 * possibly including deleted entities in the search.
	 *
	 * @param cls Class of the entity.
	 * @param entityName Name of the entity (if can't be guessed basing on the {@code cls}).
	 * @param primaryKey Primary key of the entity.
	 * @param revision Revision in which to get the entity.
	 * @param includeDeletions Whether to include deleted entities in the search.
	 * @param <T> The type of the entity to find
	 *
	 * @return The found entity instance at the given revision (its properties may be partially filled
	 *         if not all properties are audited) or null, if an entity with that id didn't exist at that
	 *         revision.
	 *
	 * @throws IllegalArgumentException If cls or primaryKey is null or revision is less or equal to 0.
	 * @throws NotAuditedException When entities of the given class are not audited.
	 * @throws IllegalStateException If the associated entity manager is closed.
	 */
	<T> T find(
			Class<T> cls, String entityName, Object primaryKey,
			Number revision, boolean includeDeletions) throws IllegalArgumentException,
			NotAuditedException, IllegalStateException;

	/**
	 * Get a list of revision numbers, at which an entity was modified.
	 *
	 * @param cls Class of the entity.
	 * @param primaryKey Primary key of the entity.
	 *
	 * @return A list of revision numbers, at which the entity was modified, sorted in ascending order (so older
	 *         revisions come first).
	 *
	 * @throws NotAuditedException When entities of the given class are not audited.
	 * @throws IllegalArgumentException If cls or primaryKey is null.
	 * @throws IllegalStateException If the associated entity manager is closed.
	 */
	List<Number> getRevisions(Class<?> cls, Object primaryKey)
			throws IllegalArgumentException, NotAuditedException, IllegalStateException;

	/**
	 * Get a list of revision numbers, at which an entity was modified, looking by entityName.
	 *
	 * @param cls Class of the entity.
	 * @param entityName Name of the entity (if can't be guessed basing on the {@code cls}).
	 * @param primaryKey Primary key of the entity.
	 *
	 * @return A list of revision numbers, at which the entity was modified, sorted in ascending order (so older
	 *         revisions come first).
	 *
	 * @throws NotAuditedException When entities of the given class are not audited.
	 * @throws IllegalArgumentException If cls or primaryKey is null.
	 * @throws IllegalStateException If the associated entity manager is closed.
	 */
	List<Number> getRevisions(Class<?> cls, String entityName, Object primaryKey)
			throws IllegalArgumentException, NotAuditedException,
			IllegalStateException;

	/**
	 * Get the date, at which a revision was created.
	 *
	 * @param revision Number of the revision for which to get the date.
	 *
	 * @return Date of commiting the given revision.
	 *
	 * @throws IllegalArgumentException If revision is less or equal to 0.
	 * @throws RevisionDoesNotExistException If the revision does not exist.
	 * @throws IllegalStateException If the associated entity manager is closed.
	 */
	Date getRevisionDate(Number revision) throws IllegalArgumentException, RevisionDoesNotExistException,
			IllegalStateException;

	/**
	 * Gets the revision number, that corresponds to the given date. More precisely, returns
	 * the number of the highest revision, which was created on or before the given date. So:
	 * <code>getRevisionDate(getRevisionNumberForDate(date)) <= date</code> and
	 * <code>getRevisionDate(getRevisionNumberForDate(date)+1) > date</code>.
	 *
	 * @param date Date for which to get the revision.
	 *
	 * @return Revision number corresponding to the given date.
	 *
	 * @throws IllegalStateException If the associated entity manager is closed.
	 * @throws RevisionDoesNotExistException If the given date is before the first revision.
	 * @throws IllegalArgumentException If <code>date</code> is <code>null</code>.
	 */
	Number getRevisionNumberForDate(Date date) throws IllegalStateException, RevisionDoesNotExistException,
			IllegalArgumentException;

	/**
	 * A helper method; should be used only if a custom revision entity is used. See also {@link RevisionEntity}.
	 *
	 * @param revisionEntityClass Class of the revision entity. Should be annotated with {@link RevisionEntity}.
	 * @param revision Number of the revision for which to get the data.
	 * @param <T> The type of the revision entity to find
	 *
	 * @return Entity containing data for the given revision.
	 *
	 * @throws IllegalArgumentException If revision is less or equal to 0 or if the class of the revision entity
	 * is invalid.
	 * @throws RevisionDoesNotExistException If the revision does not exist.
	 * @throws IllegalStateException If the associated entity manager is closed.
	 */
	<T> T findRevision(Class<T> revisionEntityClass, Number revision) throws IllegalArgumentException,
			RevisionDoesNotExistException, IllegalStateException;

	/**
	 * Find a map of revisions using the revision numbers specified.
	 *
	 * @param revisionEntityClass Class of the revision entity. Should be annotated with
	 * {@link RevisionEntity}.
	 * @param revisions Revision numbers of the revision for which to get the data.
	 * @param <T> The type of the revision entity to find
	 *
	 * @return A map of revision number and the given revision entity.
	 *
	 * @throws IllegalArgumentException If a revision number is less or equal to 0 or if the class of
	 * the revision entity is invalid.
	 * @throws IllegalStateException If the associated entity manager is closed.
	 */
	<T> Map<Number, T> findRevisions(
			Class<T> revisionEntityClass,
			Set<Number> revisions) throws IllegalArgumentException,
			IllegalStateException;

	/**
	 * Gets an instance of the current revision entity, to which any entries in the audit tables will be bound.
	 * Please note the if {@code persist} is {@code false}, and no audited entities are modified in this session,
	 * then the obtained revision entity instance won't be persisted. If {@code persist} is {@code true}, the revision
	 * entity instance will always be persisted, regardless of whether audited entities are changed or not.
	 *
	 * @param revisionEntityClass Class of the revision entity. Should be annotated with {@link RevisionEntity}.
	 * @param persist If the revision entity is not yet persisted, should it become persisted. This way, the primary
	 * identifier (id) will be filled (if it's assigned by the DB) and available, but the revision entity will be
	 * persisted even if there are no changes to audited entities. Otherwise, the revision number (id) can be
	 * {@code null}.
	 * @param <T> The type of the revision entity to find
	 *
	 * @return The current revision entity, to which any entries in the audit tables will be bound.
	 */
	<T> T getCurrentRevision(Class<T> revisionEntityClass, boolean persist);

	/**
	 * Creates an audit query
	 *
	 * @return A query creator, associated with this AuditReader instance, with which queries can be
	 *         created and later executed. Shouldn't be used after the associated Session or EntityManager
	 *         is closed.
	 */
	AuditQueryCreator createQuery();

	/**
	 * Checks if the entityClass was configured to be audited. Calling
	 * isEntityNameAudited() with the string of the class name will return the
	 * same value.
	 *
	 * @param entityClass Class of the entity asking for audit support
	 *
	 * @return true if the entityClass is audited.
	 */
	boolean isEntityClassAudited(Class<?> entityClass);

	/**
	 * Checks if the entityName was configured to be audited.
	 *
	 * @param entityName EntityName of the entity asking for audit support.
	 *
	 * @return true if the entityName is audited.
	 */
	boolean isEntityNameAudited(String entityName);


	/**
	 * @param entity that was obtained previously from the same AuditReader.
	 *
	 * @return the entityName for the given entity, null in case the entity is
	 *         not associated with this AuditReader instance.
	 */
	String getEntityName(Object primaryKey, Number revision, Object entity)
			throws HibernateException;

	/**
	 * @return Basic implementation of {@link CrossTypeRevisionChangesReader} interface. Raises an exception if the default
	 *         mechanism of tracking entity names modified during revisions has not been enabled.
	 *
	 * @throws AuditException If none of the following conditions is satisfied:
	 * <ul>
	 * <li><code>org.hibernate.envers.track_entities_changed_in_revision</code>
	 * parameter is set to <code>true</code>.</li>
	 * <li>Custom revision entity (annotated with {@link RevisionEntity})
	 * extends {@link DefaultTrackingModifiedEntitiesRevisionEntity} base class.</li>
	 * <li>Custom revision entity (annotated with {@link RevisionEntity}) encapsulates a field
	 * marked with {@link ModifiedEntityNames} interface.</li>
	 * </ul>
	 */
	public CrossTypeRevisionChangesReader getCrossTypeRevisionChangesReader() throws AuditException;
}
