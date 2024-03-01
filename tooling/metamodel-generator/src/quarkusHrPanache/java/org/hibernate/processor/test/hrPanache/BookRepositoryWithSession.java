package org.hibernate.processor.test.hrPanache;

import java.util.List;

import org.hibernate.annotations.processing.Find;
import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.hibernate.reactive.panache.common.runtime.SessionOperations;
import io.smallrye.mutiny.Uni;

public interface BookRepositoryWithSession {
	
	public default Uni<Mutiny.Session> mySession() {
		return SessionOperations.getSession();
	}
	
    @Find
    public Uni<List<PanacheBook>> findBook(String isbn);
}
