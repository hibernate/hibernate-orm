/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.internal.entities.mapper.relation.lazy;

import org.hibernate.HibernateException;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.internal.entities.mapper.relation.ToOneEntityLoader;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Tomasz Bech
 * @author HernпїЅn Chanfreau
 */
public class ToOneDelegateSessionImplementor extends AbstractDelegateSessionImplementor {
	private static final long serialVersionUID = 4770438372940785488L;

	private final AuditReaderImplementor versionsReader;
	private final Class<?> entityClass;
	private final Object entityId;
	private final Number revision;
	private final boolean removed;
	private final EnversService enversService;

	public ToOneDelegateSessionImplementor(
			AuditReaderImplementor versionsReader,
			Class<?> entityClass,
			Object entityId,
			Number revision,
			boolean removed,
			EnversService enversService) {
		super( versionsReader.getSessionImplementor() );
		this.versionsReader = versionsReader;
		this.entityClass = entityClass;
		this.entityId = entityId;
		this.revision = revision;
		this.removed = removed;
		this.enversService = enversService;
	}

	@Override
	public Object doImmediateLoad(String entityName) throws HibernateException {
		return ToOneEntityLoader.loadImmediate(
				versionsReader,
				entityClass,
				entityName,
				entityId,
				revision,
				removed,
				enversService
		);
	}
}
