// Not supported yet: https://hibernate.atlassian.net/browse/HHH-17960
//package org.hibernate.processor.test.hrPanache;
//
//import java.util.List;
//
//import org.hibernate.reactive.mutiny.Mutiny;
//
//import io.smallrye.mutiny.Uni;
//import jakarta.data.repository.CrudRepository;
//import jakarta.data.repository.Find;
//import jakarta.data.repository.Query;
//import jakarta.data.repository.Repository;
//
//@Repository
//public interface JakartaDataBookRepository extends CrudRepository<PanacheBook, Long> {
//	
//	public Uni<Mutiny.StatelessSession> session();
//	
//    @Find
//    public Uni<List<PanacheBook>> findBook(String isbn);
//
//    @Query("WHERE isbn = :isbn")
//    public Uni<List<PanacheBook>> hqlBook(String isbn);
//}
