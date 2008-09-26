package org.hibernate.test.instrument.cases;

/**
 * @author Steve Ebersole
 */
public interface Executable {
	public void prepare();
	public void execute() throws Exception;
	public void complete();
}
