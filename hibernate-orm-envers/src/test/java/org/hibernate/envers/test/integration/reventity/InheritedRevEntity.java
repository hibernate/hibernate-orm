/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity;

import javax.persistence.Entity;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdRevisionEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@RevisionEntity
public class InheritedRevEntity extends SequenceIdRevisionEntity {
}
