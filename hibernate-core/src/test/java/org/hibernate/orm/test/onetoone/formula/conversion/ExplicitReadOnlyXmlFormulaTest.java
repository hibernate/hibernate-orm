package org.hibernate.orm.test.onetoone.formula.conversion;

/// Retains the explicit read-only mapping as the inference runtime control.
///
/// @author Steve Ebersole
public class ExplicitReadOnlyXmlFormulaTest extends XmlFormulaTest {
	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/onetoone/formula/conversion/mapping-explicit-read-only.xml" };
	}
}
