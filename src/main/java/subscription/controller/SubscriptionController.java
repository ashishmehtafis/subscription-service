package subscription.controller;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import subscription.exception.ResourceNotFoundException;
import subscription.model.Book;
import subscription.model.Subscription;
import subscription.repository.SubscriptionRepository;

@RestController
@RequestMapping("/api/v1")
public class SubscriptionController {
	@Autowired
	private SubscriptionRepository subscriptionRepository;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	private KafkaTemplate<String, String> template;

	@GetMapping("/subscriptions")
	public List<Subscription> getAllSubscriptions() {
		return subscriptionRepository.findAll();
	}

	@GetMapping("/subscriptions/{id}")
	public ResponseEntity<Subscription> getSubscriptionById(@PathVariable(value = "id") String subscriptionId)
			throws ResourceNotFoundException {
		Subscription subscription = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new ResourceNotFoundException("Subscription not found for this id :: " + subscriptionId));
		return ResponseEntity.ok().body(subscription);
	}

	@PostMapping("/subscriptions")
	public Subscription createSubscription(@RequestBody Subscription subscription) {
		
		Subscription updatedSubscription = null;
		String apiGatewayUrl = getApiGatewayUrl();
		String bookGetServiceUrl = apiGatewayUrl + "books-service/api/v1/books/";
		String bookUpdateServiceUrl = apiGatewayUrl + "books-service/api/v1/books/updateavailability/";
		if(subscription.getDateReturned() == null)
		{
			bookGetServiceUrl = bookGetServiceUrl + subscription.getBookId();
			Book book = restTemplate.getForObject(bookGetServiceUrl, Book.class);
			
			if(book.getAvailableCopies() > 0)
			{
				// Decrease availability and save subscription record.
				bookUpdateServiceUrl = bookUpdateServiceUrl + subscription.getBookId() + "/" + "-1";
				restTemplate.postForEntity(bookUpdateServiceUrl, null, Book.class);
				updatedSubscription = subscriptionRepository.save(subscription);
			}
			else if (subscription.notify.toUpperCase() == "YES")
			{
				// Place in Queue
				template.send("BookNotificationTopic", subscription.getBookId(), subscription.getSubscriberId());
			}
			else
			{
				// fail the operation
				
			}
		}
		else
		{
			List<Subscription> subscriptions = subscriptionRepository.findAll();
			for (Subscription s : subscriptions)  
	        { 
	            if (s.getSubscriberId().toUpperCase() == subscription.getSubscriberId().toUpperCase()
	            		&& s.getBookId().toUpperCase() == subscription.getBookId().toUpperCase())
	            {
	            	// Increase availability
					bookUpdateServiceUrl = bookUpdateServiceUrl + subscription.getBookId() + "/" + "1";
					restTemplate.postForEntity(bookUpdateServiceUrl, null, Book.class);

					// update return date and save subscription
					s.setDateReturned(subscription.getDateReturned());
	            	updatedSubscription = subscriptionRepository.save(s);
	            	
	            	// Let the users know that book is now available
	            	notifyUser();
	            }
	        } 	
			
		}
		
		return updatedSubscription;
	}
	
	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServer;
	
	private void notifyUser() {
		Properties props = new Properties();
		props.put("bootstrap.server", bootstrapServer);
		props.put("group.id", "Group1");
		props.put("enable.auto.commit", "true");
		props.put("session.timeout.ms", "30000");
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("enable.auto.commit", "true");
		
		KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
		consumer.subscribe(Arrays.asList("BookNotificationTopic"));

		while(true) {
			ConsumerRecords<String, String> messages = consumer.poll(Duration.ofMillis(1000));
			
			for(ConsumerRecord<String, String> cr : messages) {
				String subscriberId = cr.value();
				String apiGatewayUrl = getApiGatewayUrl();
				String userGetServiceUrl = apiGatewayUrl + "books-service/api/v1/users/" + subscriberId;
				
				// Send Notification Email
				
			}
		}
	}

	@Value("${configuration.client.url}")
	private String configurationClientUrl;
	
	public String getApiGatewayUrl() {
		String apiGatewayUrl = restTemplate.getForObject(configurationClientUrl, String.class);
		return apiGatewayUrl; 
	}

	@PutMapping("/subscriptions/{id}")
	public ResponseEntity<Subscription> updateSubscription(@PathVariable(value = "id") String subscriptionId,
			@RequestBody Subscription subscriptionDetails) throws ResourceNotFoundException {
		Subscription subscription = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new ResourceNotFoundException("Subscription not found for this id :: " + subscriptionId));

		subscription.setDateSubscribed(subscriptionDetails.getDateSubscribed());
		subscription.setDateReturned(subscriptionDetails.getDateReturned());
		subscription.setBookId(subscriptionDetails.getBookId());
		final Subscription updatedSubscription = subscriptionRepository.save(subscription);
		return ResponseEntity.ok(updatedSubscription);
	}

	@DeleteMapping("/subscriptions/{id}")
	public Map<String, Boolean> deleteSubscription(@PathVariable(value = "id") String subscriptionId)
			throws ResourceNotFoundException {
		Subscription subscription = subscriptionRepository.findById(subscriptionId)
				.orElseThrow(() -> new ResourceNotFoundException("Subscription not found for this id :: " + subscriptionId));

		subscriptionRepository.delete(subscription);
		Map<String, Boolean> response = new HashMap<>();
		response.put("deleted", Boolean.TRUE);
		return response;
	}
}
