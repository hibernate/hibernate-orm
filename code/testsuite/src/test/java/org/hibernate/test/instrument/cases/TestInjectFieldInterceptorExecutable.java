package org.hibernate.test.instrument.cases;

import org.hibernate.test.instrument.domain.Document;
import org.hibernate.intercept.FieldInterceptionHelper;

import java.util.HashSet;

/**
 * @author Steve Ebersole
 */
public class TestInjectFieldInterceptorExecutable extends AbstractExecutable {
	public void execute() {
		Document doc = new Document();
		FieldInterceptionHelper.injectFieldInterceptor( doc, "Document", new HashSet(), null );
		doc.getId();
	}
}
