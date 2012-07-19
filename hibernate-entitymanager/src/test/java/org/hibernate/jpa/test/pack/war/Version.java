//$Id$
package org.hibernate.jpa.test.pack.war;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Version {
	private static final String DOT = ".";
	private int major;
	private int minor;
	private int micro;

	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}

	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public int getMicro() {
		return micro;
	}

	public void setMicro(int micro) {
		this.micro = micro;
	}

	public String toString() {
		return new StringBuffer( major ).append( DOT ).append( minor ).append( DOT ).append( micro ).toString();
	}
}
