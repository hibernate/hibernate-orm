package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * A {@code @OneToOne} whose join mixes a writable physical column with a formula.
 * The mixed tuple must retain its existing logical many-to-one binding -- it is not
 * eligible for the read-only formula-based "special one-to-one" binding path.
 *
 * @author Steve Ebersole
 */
@Entity
public class OneToOneContact {
	@Id
	private Integer id;
	private String name;
	@OneToOne
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
}
