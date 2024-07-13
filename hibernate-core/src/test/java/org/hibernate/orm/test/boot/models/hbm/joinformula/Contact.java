/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class Contact {
	@Id
	private Integer id;
	private String name;
	@ManyToOne
	@JoinColumnsOrFormulas( value = {
			@JoinColumnOrFormula(
					column = @JoinColumn(
							name = "mailing_address_fk",
							referencedColumnName = "id"
					)
			),
			@JoinColumnOrFormula(
					formula = @JoinFormula(
							value = "'MAILING'",
							referencedColumnName = "type"
					)
			) }
	)
	private Address mailingAddress;
	@ManyToOne
	@JoinColumnsOrFormulas(
			value = {
					@JoinColumnOrFormula(
							column = @JoinColumn(
									name = "shipping_address_fk",
									referencedColumnName = "id"
							)
					),
					@JoinColumnOrFormula(
							formula = @JoinFormula(
									value = "'SHIPPING'",
									referencedColumnName = "type"
							)
					)
			}
	)
	private Address shippingAddress;
}
