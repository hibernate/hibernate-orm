package org.hibernate.orm.test.onetoone.formula.conversion;

/// Runs the shared-column and formula scenarios using Hibernate mapping XML.
///
/// @author Steve Ebersole
public class XmlFormulaTest extends AnnotationFormulaTest {
	@Override
	protected void applySettings(org.hibernate.boot.registry.StandardServiceRegistryBuilder builder) {
		super.applySettings(builder);
		builder.applySetting(org.hibernate.cfg.Environment.VALIDATE_XML, true);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[0];
	}

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/onetoone/formula/conversion/mapping.xml" };
	}
}
