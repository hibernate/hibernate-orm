package org.hibernate.envers.query.criteria.internal;

import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.internal.reader.AuditReaderImplementor;
import org.hibernate.envers.internal.tools.query.Parameters;
import org.hibernate.envers.internal.tools.query.QueryBuilder;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.hibernate.envers.query.internal.property.PropertyNameGetter;

public class IlikeAuditExpression implements AuditCriterion {

	private PropertyNameGetter propertyNameGetter;
	private String value;

	public IlikeAuditExpression(PropertyNameGetter propertyNameGetter, String value) {
		this.propertyNameGetter = propertyNameGetter;
		this.value = value;
	}

	public void addToQuery(AuditConfiguration auditCfg,
			AuditReaderImplementor versionsReader, String entityName,
			QueryBuilder qb, Parameters parameters) {

		String propertyName = CriteriaTools.determinePropertyName(
				auditCfg,
				versionsReader,
				entityName,
				propertyNameGetter);
		CriteriaTools.checkPropertyNotARelation( auditCfg, entityName, propertyName );

		parameters.addWhereWithFunction( propertyName, " lower ", " like ", value.toLowerCase() );
	}

}
