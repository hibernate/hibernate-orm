/**
 * 
 */
package org.hibernate.envers.query.internal.impl;

import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
interface AuditQueryImplementor extends AuditQuery {

	void registerProjection(final String entityName, final AuditProjection projection);

}
