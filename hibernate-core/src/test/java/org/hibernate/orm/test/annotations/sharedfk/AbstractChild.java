/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.sharedfk;

import jakarta.persistence.*;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.dialect.DB2zDialect;

import static jakarta.persistence.DiscriminatorType.INTEGER;

@Entity
@Table(name = " INHERITANCE_TAB")
//@DiscriminatorColumn(name = "DISC")
@DiscriminatorFormula(discriminatorType = INTEGER,
		value = "CASE WHEN VALUE1 IS NOT NULL THEN 1 WHEN VALUE2 IS NOT NULL THEN 2 END")
// DB2 z/OS doesn't seem to support case expressions in a check constraint
// and since the formula ends up as a part of the table definition check constraint,
// we need to override the expression to use decode instead
@DialectOverride.DiscriminatorFormula(dialect = DB2zDialect.class,
		override = @DiscriminatorFormula(value = "DECODE(VALUE1, cast(null as varchar(255)),2,1)", discriminatorType = INTEGER))
public abstract class AbstractChild {
	@Id
	@GeneratedValue
	@Column(name = "ID")
	Integer id;
}
