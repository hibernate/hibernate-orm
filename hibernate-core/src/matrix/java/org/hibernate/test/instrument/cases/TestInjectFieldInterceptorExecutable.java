package org.hibernate.test.instrument.cases;
import java.util.HashSet;

import org.hibernate.bytecode.instrumentation.internal.FieldInterceptionHelper;
import org.hibernate.test.instrument.domain.Document;

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
