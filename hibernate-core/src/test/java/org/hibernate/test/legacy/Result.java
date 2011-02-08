package org.hibernate.test.legacy;



public class Result {
	private String name;
	private long amount;
	private long count;
	/**
	 * Returns the amount.
	 * @return long
	 */
	public long getAmount() {
		return amount;
	}

	/**
	 * Returns the count.
	 * @return int
	 */
	public long getCount() {
		return count;
	}

	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the amount.
	 * @param amount The amount to set
	 */
	public void setAmount(long amount) {
		this.amount = amount;
	}

	/**
	 * Sets the count.
	 * @param count The count to set
	 */
	public void setCount(long count) {
		this.count = count;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public Result(String n, long a, int c) {
		name = n;
		amount = a;
		count = c;
	}
	
	public Result(String n, long a, long c) {
		name = n;
		amount = a;
		count = c;
	}
}
