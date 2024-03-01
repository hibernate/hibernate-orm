package org.hibernate.processor.test.hrPanache;

import java.util.List;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PanacheBookRepository implements PanacheRepository<PanacheBook> {
    @Find
    public native Uni<List<PanacheBook>> findBook(String isbn);

    @HQL("WHERE isbn = :isbn")
    public native Uni<List<PanacheBook>> hqlBook(String isbn);
}
