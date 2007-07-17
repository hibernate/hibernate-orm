package org.hibernate.param;

/**
 * An additional contract for parameters which originate from
 * parameters explicitly encountered in the source statement
 * (HQL or native-SQL).
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public interface ExplicitParameterSpecification extends ParameterSpecification {
	public int getSourceLine();
	public int getSourceColumn();
}
