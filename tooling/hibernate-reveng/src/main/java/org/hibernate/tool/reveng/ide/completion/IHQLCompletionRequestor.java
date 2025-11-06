/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.ide.completion;


/**
 * The interface to implement to collect completion proposals.
 *
 * @author Max Rydahl Andersen
 *
 */
public interface IHQLCompletionRequestor {

	boolean accept(HQLCompletionProposal proposal);

	void completionFailure(String errorMessage);

}
