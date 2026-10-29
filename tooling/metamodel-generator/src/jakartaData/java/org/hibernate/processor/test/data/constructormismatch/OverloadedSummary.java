package org.hibernate.processor.test.data.constructormismatch;

import java.util.List;

public class OverloadedSummary {
	public OverloadedSummary(String title) {
	}

	public OverloadedSummary(List<String> titles, int[] counts) {
	}

	public OverloadedSummary(int value1) {
	}

	public OverloadedSummary(int value1, int value2) {
	}

	public OverloadedSummary(int value1, int value2, int value3) {
	}

	public OverloadedSummary(int value1, int value2, int value3, int value4) {
	}

	public OverloadedSummary(int value1, int value2, int value3, int value4, int value5) {
	}

	public OverloadedSummary(int value1, int value2, int value3, int value4, int value5, int value6) {
	}

	public OverloadedSummary(int value1, int value2, int value3, int value4, int value5, int value6, int value7) {
	}

	public OverloadedSummary(int value1, int value2, int value3, int value4, int value5, int value6, int value7, int value8) {
	}

	public OverloadedSummary(int value1, int value2, int value3, int value4, int value5, int value6, int value7, int value8, int value9) {
	}

	public OverloadedSummary(int value1, int value2, int value3, int value4, int value5, int value6, int value7, int value8, int value9, int value10) {
	}
}
