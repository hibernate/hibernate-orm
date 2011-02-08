package org.hibernate.test.bytecode;
import java.text.ParseException;
import java.util.Date;

/**
 * @author Steve Ebersole
 */
public class Bean {
	private String someString;
	private Long someLong;
	private Integer someInteger;
	private Date someDate;
	private long somelong;
	private int someint;
	private Object someObject;


	public String getSomeString() {
		return someString;
	}

	public void setSomeString(String someString) {
		this.someString = someString;
	}

	public Long getSomeLong() {
		return someLong;
	}

	public void setSomeLong(Long someLong) {
		this.someLong = someLong;
	}

	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}

	public Date getSomeDate() {
		return someDate;
	}

	public void setSomeDate(Date someDate) {
		this.someDate = someDate;
	}

	public long getSomelong() {
		return somelong;
	}

	public void setSomelong(long somelong) {
		this.somelong = somelong;
	}

	public int getSomeint() {
		return someint;
	}

	public void setSomeint(int someint) {
		this.someint = someint;
	}

	public Object getSomeObject() {
		return someObject;
	}

	public void setSomeObject(Object someObject) {
		this.someObject = someObject;
	}

	public void throwException() throws ParseException {
		throw new ParseException( "you asked for it...", 0 );
	}
}
