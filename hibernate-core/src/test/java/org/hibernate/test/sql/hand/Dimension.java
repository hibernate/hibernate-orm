package org.hibernate.test.sql.hand;


/**
 * Used to mimic some tests from the JPA testsuite...
 *
 * @author Steve Ebersole
 */
public class Dimension {
	private Long id;
	private int length;
	private int width;

	public Dimension() {}

	public Dimension(int length, int width) {
		this.length = length;
		this.width = width;
	}

	public Long getId() {
		return id;
	}

	public int getLength() {
		return length;
	}

	public int getWidth() {
		return width;
	}
}
