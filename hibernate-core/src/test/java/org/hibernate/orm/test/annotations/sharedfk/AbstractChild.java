/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.sharedfk;

import jakarta.persistence.*;
import org.hibernate.annotations.DiscriminatorFormula;

import static jakarta.persistence.DiscriminatorType.INTEGER;

@Entity
@Table(name = " INHERITANCE_TAB")
//@DiscriminatorColumn(name = "DISC")
@DiscriminatorFormula(discriminatorType = INTEGER,
		value = "CASE WHEN VALUE1 IS NOT NULL THEN 1 WHEN VALUE2 IS NOT NULL THEN 2 END")
public abstract class AbstractChild {
	@Id
	@GeneratedValue
	@Column(name = "ID")
	Integer id;
}
