package org.hibernate.tool.ide.completion;


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
