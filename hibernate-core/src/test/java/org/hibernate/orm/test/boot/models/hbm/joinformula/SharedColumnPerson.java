package org.hibernate.orm.test.boot.models.hbm.joinformula;

import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

/**
 * A one-to-one join combining the read-only owner identifier column with a formula.
 *
 * @author Steve Ebersole
 */
@Entity
public class SharedColumnPerson {
	@Id
	private String name;

	@OneToOne(optional = true, cascade = CascadeType.ALL)
	@JoinColumnsOrFormulas({
			@JoinColumnOrFormula(column = @JoinColumn(
					name = "name", referencedColumnName = "personName",
					insertable = false, updatable = false)),
			@JoinColumnOrFormula(formula = @JoinFormula(
					value = "'HOME'", referencedColumnName = "addressType"))
	})
	private SharedColumnAddress address;
}
