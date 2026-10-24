package org.hibernate.processor.test.data.constructormismatch;

import java.util.List;

public class OverloadedSummary {
	public OverloadedSummary(String title) {
	}

	public OverloadedSummary(List<String> titles, int[] counts) {
	}
}
