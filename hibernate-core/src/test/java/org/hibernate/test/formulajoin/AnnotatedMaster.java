/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.formulajoin;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;

/**
 * @author Steve Ebersole
 */
@Entity
public class AnnotatedMaster {
	@Id
	private Integer id;
	private String name;
	@ManyToOne(fetch= FetchType.EAGER, optional=false)
	@JoinColumnOrFormula(formula=@JoinFormula(value="my_domain_key'", referencedColumnName="detail_domain"))
	@JoinColumnOrFormula(column=@JoinColumn(name="detail", referencedColumnName="id"))
	@Fetch(FetchMode.JOIN)
	@NotNull
	private AnnotatedDetail detail;
}
