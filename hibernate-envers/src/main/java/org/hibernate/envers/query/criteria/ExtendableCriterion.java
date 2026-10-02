package org.hibernate.envers.query.criteria;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface ExtendableCriterion {
	ExtendableCriterion add(AuditCriterion criterion);
}
