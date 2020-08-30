DROP TABLE IF EXISTS SUbscription;
CREATE TABLE Subscription(
  subscriber_id VARCHAR(250) PRIMARY KEY,
  date_subscribed DATE,
  date_returned DATE,
  book_id VARCHAR(250)
);


INSERT INTO Subscription (subscriber_id, date_subscribed, date_returned, book_id) VALUES
('John', '2020-06-12', NULL,'B1212'),
('Mark', '2020-04-26', '2020-05-14', 'B4232'),('Peter', '2020-06-22', NULL, 'B1212');
