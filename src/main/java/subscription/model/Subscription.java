package subscription.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="subscription")
public class Subscription {

	private String subscriberId;
	private Date dateSubscribed;
	private Date dateReturned;
	private String bookId;
	public String notify;

	@Id
	@Column(name = "subscriber_id", nullable = false)
	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	@Column(name = "date_subscribed", nullable = false)
	public Date getDateSubscribed() {
		return dateSubscribed;
	}

	public void setDateSubscribed(Date dateSubscribed) {
		this.dateSubscribed = dateSubscribed;
	}

	@Column(name = "date_returned", nullable = true)
	public Date getDateReturned() {
		return dateReturned;
	}

	public void setDateReturned(Date dateReturned) {
		this.dateReturned = dateReturned;
	}

	@Column(name = "book_id", nullable = false)
	public String getBookId() {
		return bookId;
	}

	public void setBookId(String bookId) {
		this.bookId = bookId;
	}
	
	@Override
	public String toString() {
		return "Subscription [subscriberId=" + subscriberId + ", dateSubscribed=" + dateSubscribed + ", dateReturned=" + dateReturned + ", bookId=" + bookId + "]";
	}

}
