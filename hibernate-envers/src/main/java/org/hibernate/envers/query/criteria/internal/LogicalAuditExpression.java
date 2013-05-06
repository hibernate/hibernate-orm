/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class LogicalAuditExpression implements AuditCriterion {
	private AuditCriterion lhs;
	private AuditCriterion rhs;
	private String op;

	public LogicalAuditExpression(AuditCriterion lhs, AuditCriterion rhs, String op) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.op = op;
	}

	public void addToQuery(
			AuditConfiguration verCfg, AuditReaderImplementor versionsReader, String entityName,
			QueryBuilder qb, Parameters parameters) {
		Parameters opParameters = parameters.addSubParameters( op );

		lhs.addToQuery( verCfg, versionsReader, entityName, qb, opParameters.addSubParameters( "and" ) );
		rhs.addToQuery( verCfg, versionsReader, entityName, qb, opParameters.addSubParameters( "and" ) );
	}
}
