package org.hibernate.test.inheritance;

import java.io.Serializable;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Vlad Paln
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14234" )
public class DenormalizedTablePhysicalIncludedTableConstraintTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.HBM2DDL_HALT_ON_ERROR, Boolean.TRUE.toString() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				SuperClass.class,
				SubClass.class
		};
	}

	@Test
	public void testUniqueConstraintFromSupTableNotAppliedToSubTable() {
		// Unique constraint should be unique in db
		// Without fixing, exception will be thrown when unique constraint in 'SUPTABLE' is applied to 'SUBTABLE' as well
		// Both table names are uppercase to accommodate HANA dialect (see https://stackoverflow.com/questions/29974507/sap-hana-invalid-table-name)
	}

	@Entity(name = "SuperClass")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Table( name = "SUPTABLE",
			uniqueConstraints = {
					@UniqueConstraint(  name = "UK",
							columnNames = {"colOne", "colTwo"})
			}
	)
	static class SuperClass implements Serializable {

		@Id
		@GeneratedValue
		Long id;

		@Column(name = "colOne")
		Long colOne;

		@Column(name = "colTwo")
		Long colTwo;
	}

	@Entity(name = "SubClass")
	@Table( name = "SUBTABLE" )
	static class SubClass extends SuperClass {

		@Column(name = "colThree")
		Long colThree;
	}
}
