package org.hibernate.orm.test.envers.integration.inheritance.joined.relation.unidirectional;

import jakarta.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class SetEntity extends AbstractSetEntity {
}
