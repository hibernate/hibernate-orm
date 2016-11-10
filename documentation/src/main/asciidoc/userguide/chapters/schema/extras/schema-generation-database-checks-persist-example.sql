INSERT  INTO Book (isbn, price, title, id)
VALUES  ('11-11-2016', 49.99, 'High-Performance Java Persistence', 1)

-- WARN SqlExceptionHelper:129 - SQL Error: 0, SQLState: 23514
-- ERROR SqlExceptionHelper:131 - ERROR: new row for relation "book" violates check constraint "book_isbn_check"