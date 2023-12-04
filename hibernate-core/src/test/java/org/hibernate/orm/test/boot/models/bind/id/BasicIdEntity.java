/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind.id;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.TenantId;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * @author Steve Ebersole
 */
@Entity
public class BasicIdEntity {
	@Id
	private Integer id;
	@Version
	private Integer version;
	@TenantId
	private String tenantId;
	@NaturalId
	private String naturalId;

}
