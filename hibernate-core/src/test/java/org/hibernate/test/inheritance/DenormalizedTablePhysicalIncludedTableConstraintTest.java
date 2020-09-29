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
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipForDialects;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Vlad Paln
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14234" )
@SkipForDialects( {
		@SkipForDialect( value = MySQLDialect.class, comment = "skip it for it support constraint name uniqueness in table, not db" ),
		@SkipForDialect( value = MariaDBDialect.class, comment = "skip it for it support constraint name uniqueness in table, not db" )
} )
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
		// Unique constraint should be unique in db (except for MySQL and Mariadb).
		// Without fixing, exception will be thrown when unique constraint in 'supTable' is applied to 'subTable' as well
	}

	@Entity(name = "SuperClass")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Table( name = "supTable",
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
	@Table( name = "subTable" )
	static class SubClass extends SuperClass {

		@Column(name = "colThree")
		Long colThree;
	}
}
