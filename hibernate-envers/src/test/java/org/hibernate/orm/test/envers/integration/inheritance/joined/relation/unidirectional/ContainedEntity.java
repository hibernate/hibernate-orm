package org.hibernate.orm.test.envers.integration.inheritance.joined.relation.unidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "Contained")
@Audited
public class ContainedEntity extends AbstractContainedEntity {
}
